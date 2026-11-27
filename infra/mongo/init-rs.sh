#!/usr/bin/env sh
set -eu

echo "Initializing MongoDB replica set rs0..."
mongosh --host localhost:27017 --eval '
  try {
    const status = rs.status();
    if (status.ok === 1) {
      print("Replica set rs0 already initialized.");
      quit(0);
    }
  } catch (e) {
    print("Initiating replica set rs0...");
    rs.initiate({ _id: "rs0", members: [{ _id: 0, host: "localhost:27017" }] });
  }
'
