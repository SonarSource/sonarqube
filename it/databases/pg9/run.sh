#!/bin/sh
docker build . -t sonarsource/pg9
docker run -it -p 127.0.0.1:5432:5432 sonarsource/pg9 
