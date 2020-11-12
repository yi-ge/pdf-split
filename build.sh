#!/usr/bin/env bash

mvn clean package -Dmaven.test.skip=true -U

docker build -t wy373226722/pdf-split:0.0.3 .

docker tag wy373226722/pdf-split:0.0.3 wy373226722/pdf-split:latest

docker push wy373226722/pdf-split:0.0.3
docker push wy373226722/pdf-split:latest
