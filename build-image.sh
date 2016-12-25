#!/usr/bin/env bash


mvn clean package -Dmaven.test.skip=true

docker build -t ongo360/mycat:1.6.1  .