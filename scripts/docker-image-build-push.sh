# Build docker image
./scripts/docker-image-build-only.sh

# Push docker image
echo "docker-hub-password" | docker login -u geneaazul --password-stdin
docker push --all-tags geneaazul/gedcom-analyzer
