#!/bin/bash

echo "Starting Infrastructure..."
docker-compose up -d

echo "Waiting for services to initialize..."
sleep 10

echo "Starting Payment Service..."
mvn spring-boot:run
