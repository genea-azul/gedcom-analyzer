#!/bin/bash

# Install ICU4J Transliterator in local Maven repo
# https://github.com/unicode-org/icu

userMvnRepo=~/.m2/repository
projMvnRepo=./.mvn/repo

artifactProjDir=$1
artifactMvnRepoDir=$2
artifactGroupId=$3
artifactModuleDir=$4
artifactIdOnBuild=$5
artifactId=$6
artifactVersion=$7

# Install dependencies in user maven repo
./mvnw install:install-file \
      -Dfile="${artifactProjDir}${artifactModuleDir}/target/${artifactIdOnBuild}-${artifactVersion}.jar" \
      -DgroupId=${artifactGroupId} \
      -DartifactId=${artifactId} \
      -Dversion=${artifactVersion} \
      -Dpackaging=jar \
      -DgeneratePom=true \
      -DcreateChecksum=true # not working actually

# Generate checksums
userMvnRepoArtifactPathNoExt="${userMvnRepo}${artifactMvnRepoDir}/${artifactId}/${artifactVersion}/${artifactId}-${artifactVersion}"
md5sum "${userMvnRepoArtifactPathNoExt}.jar" | cut -d " " -f 1 > "${userMvnRepoArtifactPathNoExt}.jar.md5"
md5sum "${userMvnRepoArtifactPathNoExt}.pom" | cut -d " " -f 1 > "${userMvnRepoArtifactPathNoExt}.pom.md5"
sha1sum "${userMvnRepoArtifactPathNoExt}.jar" | cut -d " " -f 1 > "${userMvnRepoArtifactPathNoExt}.jar.sha1"
sha1sum "${userMvnRepoArtifactPathNoExt}.pom" | cut -d " " -f 1 > "${userMvnRepoArtifactPathNoExt}.pom.sha1"

# Copy resources to project maven repo
mkdir -p "${projMvnRepo}${artifactMvnRepoDir}/${artifactId}"
cp -r "${userMvnRepo}${artifactMvnRepoDir}/${artifactId}/${artifactVersion}" "${projMvnRepo}${artifactMvnRepoDir}/${artifactId}"
cp "${userMvnRepo}${artifactMvnRepoDir}/${artifactId}/maven-metadata-local.xml" "${projMvnRepo}${artifactMvnRepoDir}/${artifactId}"
rm "${projMvnRepo}${artifactMvnRepoDir}/${artifactId}/${artifactVersion}/_remote.repositories"
