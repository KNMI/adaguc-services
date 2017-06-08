FROM centos:7

MAINTAINER Adaguc Team at KNMI <adaguc@knmi.nl>

RUN yum update -y && yum install -y \
    epel-release

RUN yum clean all && yum groupinstall -y "Development tools"

RUN yum update -y && yum install -y \
    hdf5-devel \
    netcdf \
    netcdf-devel \
    proj \
    proj-devel \
    sqlite \
    sqlite-devel \
    udunits2 \
    udunits2-devel \
    make \
    libxml2-devel \
    cairo-devel \
    gd-devel \
    postgresql-devel \
    postgresql-server \
    gdal-devel \
    tomcat \ 
    maven
         
RUN mkdir /src




WORKDIR /src
# Configure postgres
RUN mkdir /postgresql
RUN touch /var/log/postgresql.log
RUN chown postgres: /postgresql/
RUN chown postgres: /var/log/postgresql.log
RUN runuser -l postgres -c "initdb -D /postgresql"

# Install adaguc-server
WORKDIR /src
RUN curl -L  https://github.com/KNMI/adaguc-server/archive/master.tar.gz > adaguc-server.tar.gz
RUN tar xvf adaguc-server.tar.gz
WORKDIR /src/adaguc-server-master
RUN bash compile.sh


# Install adaguc-services
WORKDIR /src
RUN mkdir adaguc-services
COPY . /src/adaguc-services

WORKDIR /src/adaguc-services
RUN mvn package 

# Configure adaguc-services
COPY ./docker/adaguc-services-config.xml /root/adaguc-services-config.xml 
ENV ADAGUC_SERVICES_CONFIG=/root/adaguc-services-config.xml 

RUN keytool -genkey -noprompt -keypass password -alias tomcat -keyalg RSA -storepass password -keystore /tmp/c4i_keystore.jks  -dname CN=compute-test.c3s-magic.eu/C=NL/O=C3SMAGIC/OU=KNMI 

# install pywps
WORKDIR /src
RUN curl -L -O https://github.com/geopython/pywps/archive/pywps-3.2.5.tar.gz
RUN tar xvf pywps-3.2.5.tar.gz

# Set up data dir, this is also configured in adaguc.docker.xml
RUN mkdir /data/

#Setup directory for automatic visualization of NetCDF's
RUN mkdir /data/adaguc-autowms

#Setup directory for visualization of ADAGUC datasets
RUN mkdir /data/adaguc-datasets

EXPOSE 8080

CMD echo "Starting POSTGRESQL DB" && \
    runuser -l postgres -c "pg_ctl -D /postgresql -l /var/log/postgresql.log start" && \
    sleep 1 && \ 
    mkdir -p /data/adaguc-autowms/ && \ 
    mkdir -p /data/adaguc-datasets/ && \ 
    cp /src/adaguc-server-master/data/datasets/testdata.nc /data/adaguc-autowms/ && \
    cp /src/adaguc-server-master/data/config/datasets/dataset_a.xml /data/adaguc-datasets/ && \
    echo "Configuring POSTGRESQL DB" && \
    runuser -l postgres -c "createuser --superuser adaguc" && \
    runuser -l postgres -c "psql postgres -c \"ALTER USER adaguc PASSWORD 'adaguc';\"" && \
    runuser -l postgres -c "psql postgres -c \"CREATE DATABASE adaguc;\"" && \
    echo "Starting TOMCAT Server" && \
    java -jar ./target/adaguc-services-1.0.0-SNAPSHOT.war

# Build with docker build -t adagucservices:alpha .
# docker run -it -p9000:8080 adagucservices:alpha bash


# This docker container needs to be runned with custom configuration settings:  
# docker network create --subnet=172.18.0.0/16 adagucnet
# docker run -i -t --net adagucnet --ip 172.18.0.2 -v $HOME/data:/data openearth/adaguc-server



# You can copy NetCDF's / GeoJSONS to your hosts ~/data directory. This will be served through adaguc-server, via the source=<filename> key value pair. testdata.nc is copied there by default. See example URL above.

