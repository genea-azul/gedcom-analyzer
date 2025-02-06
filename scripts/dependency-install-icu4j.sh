#!/bin/bash

# Install ICU4J Transliterator in local Maven repo
# https://github.com/unicode-org/icu
#
#   git clone --depth 1 -b maint/maint-76 https://github.com/unicode-org/icu

artifactProjDir=../icu/icu4j
artifactGitMain=main
artifactGitBranch=maint/maint-76
artifactVersion=76.1

# Update branch (enable if not cloning previously)
# git -C ${artifactProjDir} pull
# git -C ${artifactProjDir} checkout ${artifactGitBranch} && git -C ${artifactProjDir} rebase origin/${artifactGitBranch}

# Build project
./mvnw clean package -DskipTests -f ${artifactProjDir}/pom.xml

# Go back to main branch
# git -C ${artifactProjDir} checkout ${artifactGitMain}

# Copy dependencies to local Maven repo
./scripts/dependency-install-file.sh "../icu/icu4j" "/com/ibm/icu" "com.ibm.icu" "/main/core" "core" "icu4j-core" ${artifactVersion}
./scripts/dependency-install-file.sh "../icu/icu4j" "/com/ibm/icu" "com.ibm.icu" "/main/translit" "translit" "icu4j-translit" ${artifactVersion}
