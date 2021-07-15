#!/usr/bin/env bash

yarn clean
yarn tsc
cp package.json README.md types.d.ts build/dist
yarn cpy 'components/**/*.css' 'build/dist' --parents
