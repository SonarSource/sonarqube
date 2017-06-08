#!/bin/sh
docker build . -t sonarsource/mysql5
docker run -it -p 127.0.0.1:3306:3306 sonarsource/mysql5
