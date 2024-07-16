#!/bin/bash

# Install FamilySearch GEDCOM Java in local Maven repo
# https://github.com/FamilySearch/gedcom5-java
#
#   git clone --depth 1 -b ee3c05fe306bdc396c4d3843746cb36c21168bf3 https://github.com/FamilySearch/gedcom5-java

userMvnRepo=~/.m2/repository
projMvnRepo=./.mvn/repo

artifactProjDir=../gedcom5-java
artifactMvnRepoDir=/org/familysearch/gedcom
artifactVersion=1.14.0
artifactGitMain=master
artifactGitBranch=ee3c05fe306bdc396c4d3843746cb36c21168bf3

# Update branch (enable if not cloning previously)
# git -C ${artifactProjDir} pull
# git -C ${artifactProjDir} checkout ${artifactGitBranch} && git -C ${artifactProjDir} rebase origin/${artifactGitBranch}

# Build project
./mvnw clean package -DskipTests -f ${artifactProjDir}/pom.xml

# Go back to main branch
# git -C ${artifactProjDir} checkout ${artifactGitMain}

# Copy dependencies to local Maven repo
./mvnw install:install-file \
      -Dfile=${artifactProjDir}/target/gedcom-${artifactVersion}.jar \
      -DgroupId=org.familysearch.gedcom \
      -DartifactId=gedcom-java \
      -Dversion=${artifactVersion} \
      -Dpackaging=jar \
      -DgeneratePom=true \
      -DcreateChecksum=true

mkdir -p ${projMvnRepo}${artifactMvnRepoDir}
cp -r ${userMvnRepo}${artifactMvnRepoDir}/gedcom-java ${projMvnRepo}${artifactMvnRepoDir}
