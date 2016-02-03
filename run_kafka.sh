#!/usr/bin/env bash
docker rm zookeeper
docker rm kafka
docker run -d --name zookeeper -p 2181:2181 jplock/zookeeper:3.4.6
docker run -d --name kafka --link zookeeper:zookeeper -p 9092:9092 ches/kafka
sleep 3s
export ZK_IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' zookeeper)
docker run --rm ches/kafka kafka-topics.sh --create --topic test --replication-factor 1 --partitions 1 --zookeeper $ZK_IP
