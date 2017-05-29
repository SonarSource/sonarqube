#!/bin/sh
docker build . -t sonarsource/mssql
docker run -it -p 127.0.0.1:1433:1433 sonarsource/mssql
