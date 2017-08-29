#!/bin/sh

export VITAL_HOME=/home/centos/vitalhome

haleyHome=/home/centos/haley-server/haley-embedded-vertx

cd $haleyHome

java -jar target/haley-embedded-app-3.2.1-fat.jar 1>> haley-embedded-app.out 2>> haley-email-stream.errors.out
