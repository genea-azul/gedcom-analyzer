package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

@Component
public class RelationshipMapper {

    private static final String SEX_SUFFIX_M_VALUE = "o";
    private static final String SEX_SUFFIX_F_VALUE = "a";
    private static final String SEX_SUFFIX_X_VALUE = "o/a";

    public RelationshipDto toRelationshipDto(
            @Nullable Relationship relationship,
            boolean obfuscateCondition) {

        if (relationship == null) {
            return null;
        }

        ReferenceType referenceType;
        int generation = relationship.getGeneration();
        int grade;

        if (generation > 0) {
            grade = relationship.distanceToAncestorThisPerson();
            referenceType = grade == 0 ? ReferenceType.PARENT : ReferenceType.PIBLING;
        } else if (generation == 0) {
            grade = relationship.distanceToAncestorRootPerson() - 1;
            referenceType = grade < 0
                    ? (relationship.isInLaw() ? ReferenceType.SPOUSE : ReferenceType.SELF)
                    : (grade == 0 ? ReferenceType.SIBLING : ReferenceType.COUSIN);
        } else {
            generation = -generation;
            grade = relationship.distanceToAncestorRootPerson();
            referenceType = grade == 0 ? ReferenceType.CHILD : ReferenceType.NIBLING;
        }

        SexType spouseSex = null;
        boolean isSeparated = false;
        if (relationship.isInLaw()) {
            Integer relatedPersons = Optional
                    .ofNullable(relationship.relatedPersonIds())
                    .map(List::size)
                    .orElse(0);
            if (relatedPersons == 1) {
                String spouseId = relationship.relatedPersonIds().get(0);
                EnrichedPerson spouse = relationship
                        .person()
                        .getGedcom()
                        .getPersonById(spouseId);
                //noinspection DataFlowIssue
                spouseSex = spouse.getSex();
                isSeparated = relationship
                        .person()
                        .getSpousesWithChildren()
                        .stream()
                        .filter(spouseWithChildren -> spouseWithChildren
                                .getSpouse()
                                .map(EnrichedPerson::getId)
                                .map(spouseId::equals)
                                .orElse(false))
                        .findFirst()
                        .map(EnrichedSpouseWithChildren::isSeparated)
                        .orElse(false);
            }
        }

        return RelationshipDto.builder()
                .personIndex(relationship.person().getOrderKey())
                .personSex(relationship.person().getSex())
                .personIsAlive(relationship.person().isAlive())
                .personName(PersonUtils.obfuscateName(relationship.person(), obfuscateCondition))
                .personYearOfBirth(relationship.person().getDateOfBirth()
                        .map(date -> obfuscateCondition ? -1 : date.getYear().getValue())
                        .orElse(null))
                .personYearOfBirthIsAbout(relationship.person().getDateOfBirth()
                        .map(dateOfBirth -> dateOfBirth.getOperator() != null)
                        .orElse(false))
                .personCountryOfBirth(relationship
                        .person()
                        .getPlaceOfBirth()
                        .map(Place::country)
                        .orElse(null))
                .referenceType(referenceType)
                .generation(generation)
                .grade(grade)
                .isInLaw(relationship.isInLaw())
                .isHalf(relationship.isHalf())
                .adoptionType(relationship.adoptionType())
                .spouseSex(spouseSex)
                .isSeparated(isSeparated)
                .isDistinguishedPerson(relationship.person().isDistinguishedPerson())
                .treeSides(relationship.treeSides())
                .isObfuscated(obfuscateCondition)
                .build();
    }

    public FormattedRelationship formatInSpanish(RelationshipDto relationship, boolean onlySecondaryDescription) {
        String personIndex = Optional.ofNullable(relationship.getPersonIndex())
                .map(String::valueOf)
                .orElse(null);
        String personSex = displaySex(relationship.getPersonSex());
        String treeSide = displayTreeSide(relationship.getTreeSides());
        String personIsAlive = relationship.getPersonIsAlive() ? " " : "✝";
        String personName = displayNameInSpanish(relationship.getPersonName());
        String personYearOfBirth = Optional.ofNullable(relationship.getPersonYearOfBirth())
                .map(yearOfBirth -> displayYear(yearOfBirth, relationship.getPersonYearOfBirthIsAbout()))
                .orElse(null);
        String personCountryOfBirth = relationship.getPersonCountryOfBirth();
        String adoption = Optional.ofNullable(relationship.getAdoptionType())
                .map(AdoptionType::name)
                .orElse(null);
        String distinguishedPerson = relationship.getIsDistinguishedPerson() ? "★" : "";
        String relationshipDesc = displayRelationshipInSpanish(relationship, onlySecondaryDescription);
        return new FormattedRelationship(
                personIndex,
                personName,
                personSex,
                personIsAlive,
                personYearOfBirth,
                personCountryOfBirth,
                adoption,
                distinguishedPerson,
                treeSide,
                relationshipDesc,
                relationship.getIsObfuscated());
    }

    public String displayNameInSpanish(String name) {
        // Only the name is private, surname is not obfuscated
        name = name.replace("<private>", "<nombre privado>");
        name = name.replace("<no name>", "<nombre desconocido>");
        return name.replace("<no spouse>", "<sin pareja>");
    }

    private String displaySex(SexType sex) {
        return switch (sex) {
            case F -> "♀";
            case M -> "♂";
            default -> " ";
        };
    }

    private String displayTreeSide(@Nullable Set<TreeSideType> treeSides) {
        if (treeSides == null) {
            return " ";
        }
        // Relationship for only one side of the tree (most common case)
        if (treeSides.size() == 1) {
            TreeSideType treeSide = treeSides.stream().findFirst().orElseThrow();
            return switch (treeSide) {
                case FATHER -> "←";
                case MOTHER -> "→";
                case DESCENDANT -> "↓";
                case SPOUSE -> "◇";
            };
        }
        /*
         * Relationship given two sides of the tree:
         *   father + mother      ->  ↔
         *   father + spouse      ->  ◁
         *   mother + spouse      ->  ▷
         *   father + descendant  ->  ↙
         *   mother + descendant  ->  ↘
         *   spouse + descendant  (not possible?)
         */
        if (treeSides.size() == 2) {
            if (treeSides.contains(TreeSideType.DESCENDANT)) {
                if (treeSides.contains(TreeSideType.FATHER)) {
                    return "↙";
                }
                if (treeSides.contains(TreeSideType.MOTHER)) {
                    return "↘";
                }
            } else if (treeSides.contains(TreeSideType.SPOUSE)) {
                if (treeSides.contains(TreeSideType.FATHER)) {
                    return "◁";
                }
                if (treeSides.contains(TreeSideType.MOTHER)) {
                    return "▷";
                }
            } else {
                return "↔";
            }
        /*
         * Relationship given three sides of the tree:
         *   father + mother + spouse      ->  ▽
         *   father + mother + descendant  ->  ⇊
         *   father + spouse + descendant  (not possible?)
         *   mother + spouse + descendant  (not possible?)
         */
        } else if (treeSides.size() == 3) {
            if (treeSides.contains(TreeSideType.SPOUSE)) {
                return "▽";
            }
            if (treeSides.contains(TreeSideType.DESCENDANT)) {
                return "⇊";
            }
        }
        return " ";
    }

    private String displayYear(Integer year, @Nullable Boolean yearIsAbout) {
        if (year == -1) {
            return "----"; // obfuscated
        }
        return Boolean.TRUE.equals(yearIsAbout)
                ? "~" + year
                : year.toString();
    }

    private String displayRelationshipInSpanish(RelationshipDto relationship, boolean onlySecondaryDescription) {
        if (relationship.getReferenceType() == ReferenceType.SELF) {
            Assert.isTrue(relationship.getGeneration() == 0, "Generation should be 0");
            Assert.isTrue(relationship.getGrade() == -1, "Grade should be -1");
            Assert.isTrue(!relationship.getIsHalf(), "isHalf should be false");
            Assert.isTrue(!relationship.getIsInLaw(), "isInLaw should be false");
            Assert.isNull(relationship.getAdoptionType(), "adoptionType should be null");
            Assert.isNull(relationship.getTreeSides(), "treeSides should be null");
            return "persona principal";
        }

        String separated = (relationship.getIsSeparated() ? "ex-" : "");

        if (relationship.getReferenceType() == ReferenceType.SPOUSE) {
            Assert.isTrue(relationship.getGeneration() == 0, "Generation should be 0");
            Assert.isTrue(relationship.getGrade() == -1, "Grade should be -1");
            Assert.isTrue(!relationship.getIsHalf(), "isHalf should be false");
            Assert.isTrue(relationship.getIsInLaw(), "isInLaw should be true");
            Assert.isNull(relationship.getAdoptionType(), "adoptionType should be null");
            return separated + "pareja";
        }

        String spousePrefix = (relationship.getIsInLaw() ? separated + "pareja de " : "");

        if (relationship.getReferenceType() == ReferenceType.PARENT) {
            Assert.isTrue(relationship.getGeneration() > 0, "Generation should be greater than 0");
            Assert.isTrue(relationship.getGrade() == 0, "Grade should be 0");
            Assert.isTrue(!relationship.getIsHalf(), "isHalf should be false");

            if (relationship.getGeneration() == 1) {
                String sexSuffix = getSexSuffixInSpanish(relationship);
                String relationshipName = getRelationshipNameInSpanish(relationship.getGeneration(), sexSuffix, Sort.Direction.ASC);
                String adoptionSuffix = getAdoptionSuffixInSpanish(relationship.getAdoptionType(), sexSuffix);
                return spousePrefix + relationshipName + adoptionSuffix;
            }

            String sexSuffix = getSexSuffixInSpanish(relationship);

            String or = "";
            if (relationship.getGeneration() >= 10) {
                String relationshipNameOr = "ancestro directo de " + relationship.getGeneration() + " generaciones";
                or = spousePrefix + relationshipNameOr;
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            String relationshipName = getRelationshipNameInSpanish(relationship.getGeneration(), sexSuffix, Sort.Direction.ASC);
            return spousePrefix + relationshipName + or;
        }

        if (relationship.getReferenceType() == ReferenceType.CHILD) {
            Assert.isTrue(relationship.getGeneration() > 0, "Generation should be greater than 0");
            Assert.isTrue(relationship.getGrade() == 0, "Grade should be 0");
            Assert.isTrue(!relationship.getIsHalf(), "isHalf should be false");

            // Special case for child's spouse naming
            if (relationship.getGeneration() <= 3) {
                if (relationship.getIsInLaw()
                        && relationship.getAdoptionType() == null
                        && relationship.getPersonSex() != SexType.U) {
                    String relationshipName = relationship.getPersonSex() == SexType.M ? "yerno" : "nuera";
                    String relationshipNamePrefix = switch (relationship.getGeneration()) {
                        case 2 -> "pro";
                        case 3 -> "ab";
                        default -> "";
                    };
                    return separated + relationshipNamePrefix + relationshipName;
                }

                if (relationship.getGeneration() == 1) {
                    String sexSuffix = getSexSuffixInSpanish(relationship);
                    String relationshipName = getRelationshipNameInSpanish(relationship.getGeneration(), sexSuffix, Sort.Direction.DESC);
                    String adoptionSuffix = getAdoptionSuffixInSpanish(relationship.getAdoptionType(), sexSuffix);
                    return spousePrefix + relationshipName + adoptionSuffix;
                }
            }

            String sexSuffix = getSexSuffixInSpanish(relationship);

            String or = "";
            if (relationship.getGeneration() >= 10) {
                String relationshipNameOr = "descendiente directo de " + relationship.getGeneration() + " generaciones";
                or = spousePrefix + relationshipNameOr;
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            String relationshipName = getRelationshipNameInSpanish(relationship.getGeneration(), sexSuffix, Sort.Direction.DESC);
            return spousePrefix + relationshipName + or;
        }

        if (relationship.getReferenceType() == ReferenceType.SIBLING) {
            Assert.isTrue(relationship.getGeneration() == 0, "Generation should be 0");
            Assert.isTrue(relationship.getGrade() == 0, "Grade should be 0");

            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";

            // Special case for sibling's spouse naming
            if (relationship.getIsInLaw() && !relationship.getIsHalf()) {
                String sexSuffix = getSexSuffixInSpanish(relationship.getPersonSex());
                return separated + halfPrefix + "cuñad" + sexSuffix;
            }

            String sexSuffix = getSexSuffixInSpanish(relationship);

            String relationshipName = "herman" + sexSuffix;
            return spousePrefix + halfPrefix + relationshipName;
        }

        if (relationship.getReferenceType() == ReferenceType.COUSIN) {
            Assert.isTrue(relationship.getGeneration() == 0, "Generation should be 0");
            Assert.isTrue(relationship.getGrade() > 0, "Grade should be greater than 0");

            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGrade(), sexSuffix);

            String relationshipName = "prim" + sexSuffix;
            return spousePrefix + halfPrefix + relationshipName + gradeSuffix;
        }

        if (relationship.getReferenceType() == ReferenceType.PIBLING) {
            Assert.isTrue(relationship.getGeneration() > 0, "Generation should be greater than 0");
            Assert.isTrue(relationship.getGrade() > 0, "Grade should be greater than 0");

            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGrade(), sexSuffix);

            String relationshipNamePrefix = "tí" + sexSuffix;

            String or = "";
            if (relationship.getGeneration() > 2 || relationship.getGrade() >= 2) {
                String relationshipNameOr1 = (relationship.getGeneration() == 1)
                        ? getTreeSideInSpanish(relationship.getTreeSides(), "padre/madre")
                        : getRelationshipNameInSpanish(relationship.getGeneration(), SEX_SUFFIX_X_VALUE, Sort.Direction.ASC);
                String adoptionSuffixOr = (relationship.getGeneration() == 1)
                        ? getAdoptionSuffixInSpanish(relationship.getAdoptionType(), getSexSuffixByParentInSpanish(relationshipNameOr1))
                        : "";
                String relationshipNameOr2 = (relationship.getGrade() == 1 ? "herman" : "prim") + sexSuffix;
                String gradeSuffixOr = getGradeSuffixInSpanish(relationship.getGrade() - 1, sexSuffix);
                or = spousePrefix + halfPrefix + relationshipNameOr2 + gradeSuffixOr + " de " + relationshipNameOr1 + adoptionSuffixOr;
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            String relationshipName = getRelationshipNameInSpanish(relationship.getGeneration(), relationshipNamePrefix, sexSuffix, Sort.Direction.ASC);
            return spousePrefix + halfPrefix + relationshipName + gradeSuffix + or;
        }

        if (relationship.getReferenceType() == ReferenceType.NIBLING) {
            Assert.isTrue(relationship.getGeneration() > 0, "Generation should be greater than 0");
            Assert.isTrue(relationship.getGrade() > 0, "Grade should be greater than 0");

            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGrade(), sexSuffix);

            String relationshipNamePrefix = "sobrin" + sexSuffix;

            String or = "";
            if (relationship.getGeneration() > 2 || relationship.getGrade() >= 2) {
                String relationshipNameOr1 = getRelationshipNameInSpanish(relationship.getGeneration(), sexSuffix, Sort.Direction.DESC);
                String relationshipNameOr2 = (relationship.getGrade() == 1 ? "herman" : "prim") + SEX_SUFFIX_X_VALUE;
                String adoptionSuffixOr = (relationship.getGeneration() == 1)
                        ? getAdoptionSuffixInSpanish(relationship.getAdoptionType(), sexSuffix)
                        : "";
                String gradeSuffixOr = getGradeSuffixInSpanish(relationship.getGrade() - 1, SEX_SUFFIX_X_VALUE);
                or = spousePrefix + relationshipNameOr1 + adoptionSuffixOr + " de " + halfPrefix + relationshipNameOr2 + gradeSuffixOr;
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            String relationshipName = getRelationshipNameInSpanish(relationship.getGeneration(), relationshipNamePrefix, sexSuffix, Sort.Direction.DESC);
            return spousePrefix + halfPrefix + relationshipName + gradeSuffix + or;
        }

        return "familiar";
    }

    private String getRelationshipNameInSpanish(
            int generation,
            String sexSuffix,
            Sort.Direction direction) {
        return getRelationshipNameInSpanish(generation, "", sexSuffix, direction);
    }

    private String getRelationshipNameInSpanish(
            int generation,
            String relationshipNamePrefix,
            String sexSuffix,
            Sort.Direction direction) {

        if (generation == 0) {
            throw new IllegalArgumentException("Invalid generation value: " + generation);
        }

        String relationshipName = switch (direction) {
            case ASC -> switch (generation) {
                case 1 -> relationshipNamePrefix.isEmpty()
                        ? switch (sexSuffix) {
                            case SEX_SUFFIX_M_VALUE -> "padre";
                            case SEX_SUFFIX_F_VALUE -> "madre";
                            default -> "padre/madre";
                        }
                        : relationshipNamePrefix;
                case 2 -> "abuel" + sexSuffix;
                case 3 -> "bisabuel" + sexSuffix;
                case 4 -> "tatarabuel" + sexSuffix;
                case 5 -> "trastatarabuel" + sexSuffix;
                default -> {
                    String gradePrefix = getGradePrefixInSpanish(generation - 1, false);
                    yield gradePrefix + "abuel" + sexSuffix;
                }
            };
            case DESC -> switch (generation) {
                case 1 -> relationshipNamePrefix.isEmpty()
                        ? "hij" + sexSuffix
                        : relationshipNamePrefix;
                case 2 -> "niet" + sexSuffix;
                case 3 -> "bisniet" + sexSuffix;
                case 4 -> "tataraniet" + sexSuffix;
                case 5 -> "trastataraniet" + sexSuffix;
                default -> {
                    String gradePrefix = getGradePrefixInSpanish(generation - 1, true);
                    yield gradePrefix + "niet" + sexSuffix;
                }
            };
        };

        if (generation == 1 || relationshipNamePrefix.isEmpty()) {
            return relationshipName;
        }

        return relationshipNamePrefix + "-" + relationshipName;
    }

    private String getTreeSideInSpanish(
            @Nullable Set<TreeSideType> treeSides,
            @SuppressWarnings("SameParameterValue") String defaultValue) {
        if (treeSides == null) {
            return defaultValue;
        }
        if (treeSides.containsAll(List.of(TreeSideType.FATHER, TreeSideType.MOTHER))) {
            return "padre/madre";
        }
        if (treeSides.contains(TreeSideType.FATHER)) {
            return "padre";
        }
        if (treeSides.contains(TreeSideType.MOTHER)) {
            return "madre";
        }
        return defaultValue;
    }

    private String getSexSuffixInSpanish(RelationshipDto relationship) {
        if (relationship.getIsInLaw()) {
            return getSexSuffixInSpanish(relationship.getSpouseSex());
        }
        return getSexSuffixInSpanish(relationship.getPersonSex());
    }

    private String getSexSuffixInSpanish(SexType sex) {
        return switch (sex) {
            case M -> SEX_SUFFIX_M_VALUE;
            case F -> SEX_SUFFIX_F_VALUE;
            default -> SEX_SUFFIX_X_VALUE;
        };
    }

    private String getSexSuffixByParentInSpanish(String parentRelationshipName) {
        return switch (parentRelationshipName) {
            case "padre" -> SEX_SUFFIX_M_VALUE;
            case "madre" -> SEX_SUFFIX_F_VALUE;
            default -> SEX_SUFFIX_X_VALUE;
        };
    }

    private String getGradeSuffixInSpanish(int grade, String sexSuffix) {
        if (grade <= 1) {
            return "";
        }
        if (grade > 20) {
            return " de " + grade + "° grado";
        }
        String ordinal = switch (grade) {
            case 2 -> "2d";
            case 3 -> "3r";
            case 4 -> "4t";
            case 5 -> "5t";
            case 6 -> "6t";
            case 7 -> "7m";
            case 8 -> "8v";
            case 9 -> "9n";
            case 10 -> "10m";
            case 11 -> "11m";
            case 12 -> "12m";
            case 13 -> "13r";
            case 14 -> "14t";
            case 15 -> "15t";
            case 16 -> "16t";
            case 17 -> "17m";
            case 18 -> "18v";
            case 19 -> "19n";
            case 20 -> "20m";
            default -> throw new IllegalStateException("Unexpected value: " + grade);
        };
        return " " + ordinal + sexSuffix;
    }

    private String getGradePrefixInSpanish(int grade, boolean appendConnector) {
        if (grade <= 1) {
            return "";
        }
        if (grade > 20) {
            return grade + "-";
        }
        return switch (grade) {
            case 2 -> "bi";
            case 3 -> "tri";
            case 4 -> "tetr" + (appendConnector ? "a" : "");
            case 5 -> "pent" + (appendConnector ? "a" : "");
            case 6 -> "hex" + (appendConnector ? "a" : "");
            case 7 -> "hept" + (appendConnector ? "a" : "");
            case 8 -> "oct" + (appendConnector ? "a" : "");
            case 9 -> "ene" + (appendConnector ? "a" : "");
            case 10 -> "dec" + (appendConnector ? "a" : "");
            case 11 -> "endec" + (appendConnector ? "a" : "");
            case 12 -> "dodec" + (appendConnector ? "a" : "");
            case 13 -> "tridec" + (appendConnector ? "a" : "");
            case 14 -> "tetradec" + (appendConnector ? "a" : "");
            case 15 -> "pentadec" + (appendConnector ? "a" : "");
            case 16 -> "hexadec" + (appendConnector ? "a" : "");
            case 17 -> "heptadec" + (appendConnector ? "a" : "");
            case 18 -> "octadec" + (appendConnector ? "a" : "");
            case 19 -> "eneadec" + (appendConnector ? "a" : "");
            case 20 -> "icos" + (appendConnector ? "a" : "");
            default -> throw new IllegalStateException("Unexpected value: " + grade);
        };
    }

    private String getAdoptionSuffixInSpanish(@Nullable AdoptionType adoptionType, String sexSuffix) {
        if (adoptionType == null) {
            return "";
        }
        return switch (adoptionType) {
            case ADOPTIVE -> " adoptiv" + sexSuffix;
            case FOSTER -> " de crianza";
        };
    }

}
