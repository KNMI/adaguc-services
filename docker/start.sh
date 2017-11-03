#!/bin/bash


echo "Starting POSTGRESQL DB" && \
runuser -l postgres -c "pg_ctl -w -D /postgresql -l /var/log/postgresql.log start" && \
mkdir -p /data/adaguc-autowms/ && \
mkdir -p /data/adaguc-datasets/ && \
mkdir -p /data/adaguc-datasets-spaces/ && \
mkdir -p /data/wpsoutputs/ && \
mkdir -p /data/adaguc-services-tmp/ && \
cp /src/adaguc-server/data/datasets/testdata.nc /data/adaguc-autowms/ && \
cp /src/adaguc-server/data/config/datasets/dataset_a.xml /data/adaguc-datasets/ && \
echo "Configuring POSTGRESQL DB" && \
runuser -l postgres -c "createuser --superuser adaguc" && \
runuser -l postgres -c "psql postgres -c \"ALTER USER adaguc PASSWORD 'adaguc';\"" && \
runuser -l postgres -c "psql postgres -c \"CREATE DATABASE adaguc;\"" && \
echo "Starting TOMCAT Server" && \
java -jar /src/adaguc-services.war