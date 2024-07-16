#!/bin/bash

# Install ICU4J Transliterator in local Maven repo
# https://github.com/unicode-org/icu
#
#   git clone --depth 1 -b maint/maint-75 https://github.com/unicode-org/icu

userMvnRepo=~/.m2/repository
projMvnRepo=./.mvn/repo

artifactProjDir=../icu/icu4j
artifactMvnRepoDir=/com/ibm/icu
artifactVersion=75.1
artifactGitMain=main
artifactGitBranch=maint/maint-75

# Update branch (enable if not cloning previously)
# git -C ${artifactProjDir} pull
# git -C ${artifactProjDir} checkout ${artifactGitBranch} && git -C ${artifactProjDir} rebase origin/${artifactGitBranch}

# Build project
./mvnw clean package -DskipTests -f ${artifactProjDir}/pom.xml

# Go back to main branch
# git -C ${artifactProjDir} checkout ${artifactGitMain}

# Copy dependencies to local Maven repo
./mvnw install:install-file \
      -Dfile=${artifactProjDir}/main/core/target/core-${artifactVersion}.jar \
      -DgroupId=com.ibm.icu \
      -DartifactId=icu4j-core \
      -Dversion=${artifactVersion} \
      -Dpackaging=jar \
      -DgeneratePom=true \
      -DcreateChecksum=true

./mvnw install:install-file \
      -Dfile=${artifactProjDir}/main/translit/target/translit-${artifactVersion}.jar \
      -DgroupId=com.ibm.icu \
      -DartifactId=icu4j-translit \
      -Dversion=${artifactVersion} \
      -Dpackaging=jar \
      -DgeneratePom=true \
      -DcreateChecksum=true

mkdir -p ${projMvnRepo}${artifactMvnRepoDir}
cp -r ${userMvnRepo}${artifactMvnRepoDir}/icu4j-core ${projMvnRepo}${artifactMvnRepoDir}
cp -r ${userMvnRepo}${artifactMvnRepoDir}/icu4j-translit ${projMvnRepo}${artifactMvnRepoDir}
