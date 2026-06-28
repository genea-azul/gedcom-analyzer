# gedcom-analyzer — CLAUDE.md

Spring Boot genealogy analyzer for Argentine/Spanish-language family trees. Reads GEDCOM files, builds an enriched in-memory graph, exposes REST search/tree APIs, and generates filtered sub-GEDCOMs and family tree exports (PDF, JSON graph).

---

## Package structure

```text
com.geneaazul.gedcomanalyzer
├── controller/          REST endpoints
├── service/
│   ├── PersonService            DFS traversal engine
│   ├── FamilyTreeHelper         Orchestration: builds relationship lists for tree/sub-GEDCOM
│   ├── GedcomParsingService     GEDCOM parsing, sub-GEDCOM filtering (format()), writing
│   ├── SearchService            All person/surname/connection searches
│   ├── GedcomAnalyzerService    GEDCOM-wide statistics and analytics
│   ├── ConnectionService        Shortest-path between two people
│   ├── SurnameService           Surname variant queries
│   ├── BirthdayService          Birthday/ephemerides
│   ├── TreeBuilderService       User-submitted tree-builder requests
│   ├── FamilyService            Family search persistence/retrieval
│   ├── familytree/              Tree exporters: PDF, HTML Pyvis, JSON graph
│   └── storage/
│       └── GedcomHolder         Singleton GEDCOM holder (BlockingQueue)
├── model/               Core domain models (Relationship, EnrichedPerson, …)
├── domain/              JPA entities (SearchFamily, SearchConnection, TreeBuilderSubmission)
├── mapper/              DTO mappers
├── repository/          Spring Data JPA repositories
├── config/              Spring configuration
└── utils/               RelationshipUtils, NameUtils, SetUtils, …
```

---

## GEDCOM loading

**`GedcomHolder`** holds a single `EnrichedGedcom` in a `LinkedBlockingQueue<EnrichedGedcom>` (capacity 1).

- `getGedcom()` — polls with 30 s timeout; blocks if still loading.
- `reloadFromStorage(refreshCachedGedcom)` — clears queue, reloads from Local or Google Drive storage.

**`EnrichedGedcom`** wraps the legacy GEDCOM with:

- All `EnrichedPerson` objects and multiple lookup indexes (by ID, UUID, surname+sex, surname+sex+birth/death year).
- Aggregated stats: families, male/female/alive/deceased counts, Azul-specific counts.
- Birthday indexes for ephemerides.
- Properties bag (`GedcomAnalyzerProperties`).

---

## Core model: `Relationship` (record)

Every person in the tree is described by a `Relationship` relative to the root person.

| Field | Type | Meaning |
|---|---|---|
| `person` | `EnrichedPerson` | The related person |
| `distanceToAncestorRootPerson` | `int` | Hops **up** from root to common ancestor (`asc`) |
| `distanceToAncestorThisPerson` | `int` | Hops **down** from common ancestor to this person (`desc`) |
| `isInLaw` | `boolean` | Reached via a SAME (spouse) hop |
| `isSpouseFamily` | `boolean` | Reached through a secondary spouse-ancestor traversal |
| `isHalf` | `boolean` | Half-sibling boundary crossed |
| `adoptionTypeAsc` | `AdoptionType` | Adoptive/foster on the ascending path |
| `adoptionTypeDesc` | `AdoptionType` | Adoptive/foster on the descending path |
| `treeSides` | `Set<TreeSideType>` | FATHER / MOTHER / SPOUSE / DESCENDANT |
| `relatedPersonIds` | `List<Integer>` | Linking person IDs (common ancestor or linking spouse) |

**Derived methods:**

- `isDirect()` → `asc == 0 || desc == 0` — pure ancestors, root, and pure descendants.
- `getDistance()` → `asc + desc`
- `getGeneration()` → `asc − desc`

**Examples:**

- Grandparent: `asc=2, desc=0`
- Cousin: `asc=2, desc=2`
- Sibling: `asc=1, desc=1`
- Child: `asc=0, desc=1`
- Spouse: `asc=0, desc=0, isInLaw=true`
- Spouse's grandparent: `asc=2, desc=0, isInLaw=true, isSpouseFamily=true`

---

## `Relationships` collection

Wraps 1–2 `Relationship` alternatives for the same person (e.g. a person who is both a blood cousin and an in-law).

Inner enum **`VisitedRelationshipTraversalStrategy`** — controls what happens when a person is visited a second time via a different path:

| Strategy                                                   | Meaning                                                                                                 |
|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| `SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW`             | Drop in-law if same-distance blood path exists                                                          |
| `SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW`              | Drop in-law if any blood path exists                                                                    |
| `CLOSEST_SKIPPING_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW`       | Keep shortest only, skip in-law if blood exists                                                         |
| `CLOSEST_KEEPING_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW` | **Used in main traversal.** Keep shortest; if an in-law path is *closer* than any blood path, keep both |

---

## Traversal engine (`PersonService`)

### Direction semantics

| Arriving direction | Parents | Children | Spouses |
|---|---|---|---|
| `ASC` | ✓ (ASC) | ✓ (DESC) | ✓ (SAME) |
| `DESC` | — | ✓ (DESC) | ✓ (SAME) |
| `ONLY_ASC` | ✓ (ONLY_ASC) | — | — |
| `SAME` | — | — | — |

**`SAME` produces an empty stream** — traversal stops at spouses. This is why spouses are dead ends in the main traversal and why `includeSpouseAncestors` requires a separate secondary pass.

### Stop predicates

- **`stopTraversingPreCondition`** — evaluated *before* the visited check. Blocks all paths to a person; once blocked, no shorter path can reach it later. Use when you want to hard-exclude a subtree.
- **`stopTraversingPostCondition`** — evaluated after visiting. Blocks expansion *from* a person, but a shorter alternate path can still reach and record that person. Use when you want to cap depth without preventing the node itself from appearing.

### Hard depth cap

DFS halts at `distance == 32` (protects against stack overflow in recursive DFS).

### Tree-side propagation

When a newly discovered path brings a new `TreeSideType` to an already-visited person, `mergeTreeSides` re-propagates through all reachable relatives. Terminates because `TreeSideType` is bounded (4 values) — each person is updated at most 4 times.

### Key entry points

| Method | What it does |
|---|---|
| `setTransientProperties(person, excludeRoot)` | Full ASC traversal + writes cached stats on the person (personsCountInTree, surnamesCountInTree, ancestryCountries, ancestryGenerations, maxDistantRelationship, distinguishedPersonsInTree) |
| `getPeopleInTree(person, excludeRoot, onlyAsc, mergeTreeSides)` | Raw traversal, no stat side-effects |
| `getPeopleInTree(…, stopPre, stopPost)` | Traversal with custom stop predicates |
| `getRelationshipBetween(personA, personB)` | Single 1-hop relationship (parent, sibling, spouse, child) |

---

## Family tree orchestration (`FamilyTreeHelper`)

**`getRelationshipsWithNotInLawPriority(person, includeSpouseAncestors)`**

Two-pass build:

1. **Pass 1** — `personService.setTransientProperties(person, false)` — full ASC traversal from root person.
2. **Pass 2** (when `includeSpouseAncestors=true`) — secondary `ONLY_ASC` traversal from each spouse via `getPeopleInTree(spouse, true, true, true)`. Results are:
   - Filtered: `distance > 0` (exclude the spouse themselves, already in pass 1).
   - Deduplicated against pass 1 via a `knownIds` `HashSet` — blood relatives are never overridden.
   - Tagged via `asInLaw(r, spouse)`: sets `isInLaw=true, isSpouseFamily=true`, overrides `relatedPersonIds` to `[spouse.getId()]`.
   - Note: if two spouses share a common ancestor, only the first spouse's entry survives (first-wins; treeSides from the second spouse are silently dropped).

Final list: sorted by `Relationship.compareTo`, then sequential `orderKey` stamped on each person.

**`asInLaw` contract** — always sets `relatedPersonIds = [spouse.getId()]`. The mapper reads `relatedPersonIds[0]` to resolve the linking spouse for Spanish display labels (suegro/suegra, político/a). Never change this field to something else without updating the mapper.

---

## Sub-GEDCOM generation (`GedcomParsingService.format()`)

```java
Gedcom format(
    Gedcom gedcom,
    List<List<Relationship>> relationshipsList,
    AlivePersonFilter alivePersonFilter,      // SKIP | SHOW_SURNAME_ONLY | show all
    boolean displayOnlyBasic,                 // keep only birth/death/marriage events
    boolean directLineageOnly,                // filter: isDirect() == true only
    @Nullable Integer rootPersonId,           // written as _ROOT tag in output
    @Nullable Integer trimTriggerSize,        // null=never trim, 0=always, N=trim if size>N
    int maxAscDepth,
    int maxDescDepth,
    @Nullable Integer distantAncestorDescLimit, // desc cap for ancestors beyond maxAscDepth
    boolean includeInLawsAtMaxDescDepth,
    boolean includeInLawsAtMaxAscDepth,
    @Nullable Integer maxCollateralDescDepth) // null=no collateral restriction
```

**Collateral filter** (applied unconditionally when `maxCollateralDescDepth` is non-null, before trimming):

```
isDirect()                                              → always keep
!isDirect() AND isInLaw                                 → always exclude
!isDirect() AND !isInLaw AND desc ≤ maxCollateralDescDepth → keep
```

`directLineageOnly=true` pre-filters using `isDirect()` so collaterals never reach this filter.

**Keep condition** (when trimming is active, applied after the collateral filter):

```
(asc ≤ maxAscDepth AND desc ≤ maxDescDepth
    AND (includeInLawsAtMaxDescDepth OR desc < maxDescDepth OR !isInLaw)
    AND (includeInLawsAtMaxAscDepth  OR asc < maxAscDepth  OR !isInLaw))
OR
(distantAncestorDescLimit != null AND asc > maxAscDepth AND desc ≤ distantAncestorDescLimit)
```

### `SubGedcomConfig` (in `GeneaAzulWebResources`)

```java
record SubGedcomConfig(
    Integer personId,
    boolean directLineageOnly,
    @Nullable Integer trimTriggerSize,
    int maxAscDepth,
    int maxDescDepth,
    @Nullable Integer distantAncestorDescLimit,
    boolean includeInLawsAtMaxDescDepth,
    boolean includeInLawsAtMaxAscDepth,
    @Nullable Integer maxCollateralDescDepth,
    boolean includeSpouseAncestors)
```

Most web configs use `directLineageOnly=true, trimTriggerSize=0, maxAscDepth=1` with `maxDescDepth` 2–4. Set `includeSpouseAncestors=true` to include the root person's spouse's ancestors (ONLY_ASC pass from spouse). Set `maxCollateralDescDepth=1` (and `directLineageOnly=false`) to include blood siblings of the root person while excluding their spouses and descendants. `null` disables the collateral filter entirely (backward-compatible default).

---

## REST API endpoints

### `SearchController` — `/api/search`

| Method | Path | Description |
|---|---|---|
| `POST` | `/family` | Search by person name + spouse name; returns matches with obfuscation options |
| `POST` | `/surnames` | Search surnames with frequency |
| `POST` | `/connection` | Find relationship path between two people |
| `GET` | `/family-tree/{personUuid}/plainPdf` | Download PDF family tree |
| `GET` | `/family-tree/{personUuid}/graphJson` | Download JSON graph family tree |

### `GedcomAnalyzerController` — `/api/gedcom-analyzer`

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | API metadata (version, env, obfuscate flag) |
| `GET` | `/health` | Health check |
| `GET` | `/metadata` | GEDCOM-wide statistics |

### `BirthdayController` — `/api/birthday`

| Method | Path | Description |
|---|---|---|
| `GET` | `/azul-today` | Birthdays for today |
| `GET` | `/ephemerides-this-month` | Commemorative dates this month |

### `TreeBuilderController` — `/api/tree-builder`

| Method | Path | Description |
|---|---|---|
| `POST` | `/submit` | Submit a tree-builder request (rate-limited, returns 429 if exceeded) |

### `AdminController` — `/api/admin`

| Method | Path | Description |
|---|---|---|
| `GET` | `/gedcom-analyzer/health` | Admin health check |
| `GET` | `/gedcom-analyzer/usageStats` | Usage statistics |
| `GET` | `/gedcom-analyzer/reload` | Reload GEDCOM from storage |
| `POST` | `/gedcom-analyzer` | Analyze an uploaded GEDCOM file |
| `GET` | `/search/family/latest` | Latest family searches |
| `GET` | `/search/family/{id}/reviewed` | Mark family search reviewed |
| `GET` | `/search/family/{id}/ignored` | Mark family search ignored |
| `GET` | `/search/connection/latest` | Latest connection searches |
| `GET` | `/search/connection/{id}/reviewed` | Mark connection search reviewed |
| `GET` | `/tree-builder/latest` | Latest tree-builder submissions |

---

## Search engine (`SearchService`)

Primary search methods:

| Method | Filters |
|---|---|
| `findPersonsByNameAndSpouseName()` | given name, surname, spouse name; exact/partial |
| `findPersonsByMonthAndDayOfBirth()` | birth month/day; optional sex filter |
| `findPersonsByMonthAndDayOfDeath()` | death month/day |
| `findPersonsByDateOfBirthBetween()` | date range |
| `findPersonsByPlaceOfBirth()` | place; exact or endsWith |
| `findPersonsByPlaceOfDeath()` | place; exact or endsWith |
| `findPersonsByPlaceOfAnyEvent()` | any event (birth, marriage, residence, immigration…) |
| `findPersonsByName()` / `findPersonsByNameAndYearOfBirth()` | name ± birth year |
| `findDuplicatedPersons()` | scored duplicate detection |
| `findPersonsByYearOfDeathAndNoParents()` | data quality: deceased with no parents |
| `findAlivePersonsTooOldOrWithFamilyMembersTooOld()` | age anomaly validation |
| `findPersonsWithCustomEventFacts()` | custom GEDCOM events |
| `findPersonsWithTagExtensions()` | custom GEDCOM tags |
| `findPersonsWithNoCountryButParentsWithCountry()` | data quality: missing country |

Common filter parameters: `SexType` (M/F/U), `AlivePersonFilter`, place exact-match vs. endsWith, date operators (BEF/AFT/ABT/EST).

Place matching for `findPersonsByPlaceOfAnyEvent` includes: birth, death, marriage, residence, immigration — controlled by `includeSpousePlaces` and `includeAllChildrenPlaces` flags.

---

## Configuration properties (`GedcomAnalyzerProperties`)

**Locale / storage:**

- `zoneId` — default `America/Argentina/Buenos_Aires`
- `locale` — default `es_AR`
- `gedcomStorageLocalPath` / `gedcomStorageGoogleDriveFileId`
- `googleApiKey`

**Export limits:**

- `maxPyvisNetworkNodesToExport` — 500
- `maxGraphJsonNodesToExport` — 250

**Rate limiting (per IP):**

- `maxClientRequestsCountThreshold` — 12 requests
- `maxClientRequestsCountSpecialThreshold` — 3 (tighter, for special endpoints)
- `maxClientRequestsHoursThreshold` — 1 hour window
- `clientsWithSpecialThreshold` — IP whitelist

**Age/relationship validation thresholds:**

- `alivePersonMaxAge` — 105 years
- `parentMinAgeDiff` / `parentMaxAgeDiff` — 20 / 50 years
- `siblingMaxAgeDiff` — 30 years
- `spouseMaxAgeDiff` — 15 years
- `matchByDayMaxPeriod` / `matchByMonthMaxPeriod` / `matchByYearMaxPeriod` — duplicate detection tolerances

**Processing flags:**

- `storeFamilySearch` / `storeConnectionSearch` — persist searches to DB
- `keepReferenceToLegacyGedcom` — false by default (memory optimization; legacy GEDCOM freed after enrichment)
- `disableObfuscateLiving` — false by default
- `deleteUploadedGedcom` — false by default

**Normalized name maps** (loaded from `application.properties`, ~1000 lines):

- `normalizedGivenNamesMap` — masculine/feminine given name variants
- `normalizedSurnamesMap` — surname spelling variants
- `namePrefixesMap` — title/prefix mappings (used in personalities JSON)
