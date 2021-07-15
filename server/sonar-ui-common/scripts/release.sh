#!/usr/bin/env bash

if [ $# -eq 0 ]
  then
    echo "Missing argument for the new version to release"
    exit 1
fi

if [ ! $(which npmrc) ]
  then
    echo "You need to install npmrc and configure it correctly, check the readme. Call:"
    echo "  npm install -g npmrc"
    exit 1
fi

git stash
sed "s/## Unreleased/## Unreleased__NEW_LINE__## $1/g" CHANGELOG.md | awk '{ sub(/__NEW_LINE__/,"\n\n"); print }' > CHANGELOG.md.back
mv CHANGELOG.md.back CHANGELOG.md
sed "s/\"version\": \".*\",/\"version\": \"$1\",/g" package.json > package.json.back
mv package.json.back package.json
yarn
yarn package
npmrc npm
git add CHANGELOG.md package.json
git commit -m "Prepare version $1"
git tag $1
yarn publish ./build/dist/sonar-ui-common-v$1.tgz

if [ $? -gt 0 ]
  then
    echo "Publish failed, aborting"
    git tag -d $1
    git reset HEAD~
    git checkout .
    npmrc default
    git stash pop
    exit 1
fi

git push
git push origin $1
npmrc default
git stash pop

