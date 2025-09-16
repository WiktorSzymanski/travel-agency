#!/bin/bash

certAlias="cosmosdb-emulator"
certFilename="emulatorcert.crt"

echo "Dowloading cert..."
curl --insecure https://localhost:8081/_explorer/emulator.pem > ~/"$certFilename"

echo "Copy cert to /usr/local/share/ca-certificates/ and update-ca-certificates..."

sudo cp ~/"$certFilename" /usr/local/share/ca-certificates/
sudo update-ca-certificates --fresh

echo "Remove old cert from keystore and add new..."

keytool -delete -alias "$certAlias" -keystore $JAVA_HOME/lib/security/cacerts
keytool -import -trustcacerts -alias "$certAlias" -file ~/"$certFilename" -keystore $JAVA_HOME/lib/security/cacerts
