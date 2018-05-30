#!/usr/bin/env bash

# the user "admin" becomes root and can create organizations

curl -XPOST -u admin:admin "http://localhost:9000/api/organizations/enable_support"
