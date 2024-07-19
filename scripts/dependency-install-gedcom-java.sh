#!/bin/bash

# Install FamilySearch GEDCOM Java in local Maven repo
# https://github.com/FamilySearch/gedcom5-java
#
#   git clone --depth 1 -b 1.14.0 https://github.com/FamilySearch/gedcom5-java

artifactProjDir=../gedcom5-java
artifactGitMain=master
artifactGitBranch=1.14.0
artifactVersion=1.14.0

# Update branch (enable if not cloning previously)
# git -C ${artifactProjDir} pull
# git -C ${artifactProjDir} checkout ${artifactGitBranch} && git -C ${artifactProjDir} rebase origin/${artifactGitBranch}

# Build project
./mvnw clean package -DskipTests -f ${artifactProjDir}/pom.xml

# Go back to main branch
# git -C ${artifactProjDir} checkout ${artifactGitMain}

# Copy dependencies to local Maven repo
./scripts/dependency-install-file.sh "../gedcom5-java" "/org/familysearch/gedcom" "org.familysearch.gedcom" "" "gedcom" "gedcom-java" ${artifactVersion}
