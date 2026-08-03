# Equivalents of the IntelliJ run configs under .run/, for use without the IDE.

GEDCOM ?= ../gedcoms/genea-azul-full-gedcom.ged
# Optional: make test-gedcom-analyzer-service METHOD=someTestMethod
METHOD ?=

.PHONY: run test-gedcom-analyzer-service test-genea-azul-web-resources \
        test-surname-service test-high-load-calculations test-shortest-paths \
        test-familytree test-search-family-tree

run:
	./mvnw clean spring-boot:run \
		-Dspring-boot.run.profiles=local \
		"-Dspring-boot.run.jvmArguments=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseCompactObjectHeaders -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError"

test-gedcom-analyzer-service:
	./mvnw test -Dtest=GedcomAnalyzerServiceTests$(if $(METHOD),\#$(METHOD),) -Dgedcom-storage-local-path=$(GEDCOM)

test-genea-azul-web-resources:
	./mvnw test -Dtest=GeneaAzulWebResources$(if $(METHOD),\#$(METHOD),) -Dgedcom-storage-local-path=$(GEDCOM)

test-surname-service:
	./mvnw test -Dtest=SurnameServiceTests$(if $(METHOD),\#$(METHOD),) -Dgedcom-storage-local-path=$(GEDCOM)

test-high-load-calculations:
	./mvnw test -Dtest=HighLoadCalculationsTests$(if $(METHOD),\#$(METHOD),) -Dgedcom-storage-local-path=$(GEDCOM)

test-shortest-paths:
	./mvnw test -Dtest=HighLoadCalculationsTests#getShortestPathsToPersons -Dgedcom-storage-local-path=$(GEDCOM)

test-familytree:
	./mvnw test -Dtest='com.geneaazul.gedcomanalyzer.service.familytree.*' \
		-Dobfuscate-living=false \
		-Dgedcom-storage-local-path=$(GEDCOM)

test-search-family-tree:
	./mvnw verify -DskipTests -Dit.test=SearchControllerIT#testSearchFamilyTree -DfailIfNoTests=false \
		-Dtest.individual.givenName=Félix \
		-Dtest.individual.surname=Piazza \
		-Dtest.individual.yearOfBirth=1862 \
		-Dtest.spouse.givenName= \
		-Dtest.spouse.surname= \
		-Dtest.father.givenName= \
		-Dgedcom-storage-local-path=$(GEDCOM)
