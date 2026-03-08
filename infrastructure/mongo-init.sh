#!/usr/bin/env bash
set -euo pipefail

echo "Waiting for MongoDB to accept connections..."
until mongosh --host mongodb_1:27017 -u root -p password --authenticationDatabase admin --quiet --eval "db.adminCommand({ ping: 1 }).ok" | grep -q 1; do
  sleep 1
done
until mongosh --host mongodb_2:27017 -u root -p password --authenticationDatabase admin --quiet --eval "db.adminCommand({ ping: 1 }).ok" | grep -q 1; do
  sleep 1
done

echo "Initiating replica set (rs0)..."
mongosh --host mongodb_1:27017 -u root -p password --authenticationDatabase admin --quiet --eval '
  rs.initiate({
    _id: "rs0",
    members: [
      { _id: 0, host: "mongodb_1:27017", priority: 2 },
      { _id: 1, host: "mongodb_2:27017", priority: 1 }
    ]
  });
'

echo "Replica set status:"
mongosh --host mongodb_1:27017 -u root -p password --authenticationDatabase admin --quiet --eval 'rs.status().members.map(m => ({name:m.name, stateStr:m.stateStr}))'
