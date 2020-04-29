FROM centos/devtoolset-7-toolchain-centos7:7
USER root

RUN yum update -y && yum install -y \
  epel-release deltarpm

RUN yum update -y && yum clean all && yum groupinstall -y "Development tools"

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
  libwebp-devel \
  tomcat \
  maven \
  openssl

# Install newer numpy
RUN curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
RUN python get-pip.py
RUN pip install numpy netcdf4 six lxml    

RUN mkdir /adaguc

# Install adaguc-services from the context
WORKDIR /adaguc/adaguc-services
COPY /src/ /adaguc/adaguc-services/src/
COPY pom.xml /adaguc/adaguc-services/pom.xml
RUN mvn package
RUN cp /adaguc/adaguc-services/target/adaguc-services-*.jar /adaguc/adaguc-services.jar

# Install adaguc-server from github
WORKDIR /adaguc
ADD https://github.com/KNMI/adaguc-server/archive/master.tar.gz /adaguc/adaguc-server-master.tar.gz
RUN tar -xzvf adaguc-server-master.tar.gz

WORKDIR /adaguc/adaguc-server-master
RUN bash compile.sh

# Run adaguc-server functional tests
RUN bash runtests.sh

# Setup directories
RUN mkdir -p /data/adaguc-autowms && \
  mkdir -p /data/adaguc-datasets && \
  mkdir -p /data/adaguc-data && \
  mkdir -p /adaguc/userworkspace && \
  mkdir -p /data/adaguc-services-home && \
  mkdir -p /adaguc/basedir && \
  mkdir -p /var/log/adaguc && \
  mkdir -p /adaguc/adagucdb && \
  mkdir -p /adaguc/security && \
  mkdir -p /data/adaguc-datasets-internal

# Configure
COPY ./Docker/adaguc-server-config.xml /adaguc/adaguc-server-config.xml
COPY ./Docker/adaguc-services-config.xml /adaguc/adaguc-services-config.xml
COPY ./Docker/start.sh /adaguc/
COPY ./Docker/adaguc-server-logrotate /etc/logrotate.d/adaguc
COPY ./Docker/adaguc-server-*.sh /adaguc/
COPY ./Docker/baselayers.xml /data/adaguc-datasets-internal/baselayers.xml
RUN  chmod +x /adaguc/adaguc-server-*.sh && chmod +x /adaguc/start.sh

# Set adaguc-services configuration file
ENV ADAGUC_SERVICES_CONFIG=/adaguc/adaguc-services-config.xml 
ENV ADAGUCDB=/adaguc/adagucdb

# These volumes are configured in /adaguc/adaguc-server-config.xml
# Place your netcdfs, HDF5 and GeoJSONS here, they will be visualized with the source=<file> KVP via the URI
VOLUME /data/adaguc-autowms   
# Place your dataset XML configuration here, they will be accessible with the dataset=<dataset basename> KVP via the URI
VOLUME /data/adaguc-datasets  
# Place your netcdfs, HDF5 and GeoJSONS here you don't want to have accessible via dataset configurations.
VOLUME /data/adaguc-data      
# Loggings are save here, including logrotate
VOLUME /var/log/adaguc/       
# You can make the postgresql database persistent by externally mounting it. Database will be initialized if directory is empty.
VOLUME /adaguc/adagucdb       
# Settings for HTTPS / SSL can be set via keystore and truststore. Self signed cert will be created if nothing is provided.
VOLUME /adaguc/security

# For HTTP
EXPOSE 8080 
# For HTTPS
EXPOSE 8443 

ENTRYPOINT /adaguc/start.sh
