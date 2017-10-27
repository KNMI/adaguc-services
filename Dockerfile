FROM centos:7

MAINTAINER Adaguc Team at KNMI <adaguc@knmi.nl>

VOLUME /config
VOLUME /data

#TODO: perhaps host on a standard port (443)
EXPOSE 9000

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
    maven \
    openssl

WORKDIR /src
# Configure postgres
RUN mkdir /postgresql
RUN touch /var/log/postgresql.log
RUN chown postgres: /postgresql/
RUN chown postgres: /var/log/postgresql.log
RUN runuser -l postgres -c "initdb -D /postgresql"

# Install adaguc-server
# TODO: switch to release version and/or Conda package if possible and available
WORKDIR /src
RUN curl -L  https://github.com/KNMI/adaguc-server/archive/master.tar.gz > adaguc-server.tar.gz
RUN tar xvf adaguc-server.tar.gz
RUN mv /src/adaguc-server-master /src/adaguc-server
WORKDIR /src/adaguc-server
RUN bash compile.sh

# install pywps
WORKDIR /src
RUN curl -L -O https://github.com/geopython/pywps/archive/pywps-3.2.5.tar.gz
RUN tar xvf pywps-3.2.5.tar.gz
RUN mv pywps-pywps-3.2.5 pywps

# Install adaguc-services from the context
WORKDIR /src/adaguc-services
COPY /src/ /src/adaguc-services/src/
COPY pom.xml /src/adaguc-services/pom.xml
RUN mvn package
RUN cp /src/adaguc-services/target/adaguc-services-*.war /src/adaguc-services.war

# Configure adaguc-services
ENV ADAGUC_SERVICES_CONFIG=/config/adaguc-services-config.xml

WORKDIR /src/adaguc-services

COPY ./docker/start.sh /src/

RUN chmod +x /src/start.sh
ENTRYPOINT /src/start.sh




# You can copy NetCDF's / GeoJSONS to your hosts ~/data directory. This will be served through adaguc-server, via the source=<filename> key value pair. testdata.nc is copied there by default. See example URL above.

