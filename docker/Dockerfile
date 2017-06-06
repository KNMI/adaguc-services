FROM centos:7

# potentially use: FROM climate4impact-portal

MAINTAINER Adaguc Team at KNMI <adaguc@knmi.nl>

RUN yum update -y && yum install -y \
    git \
    hdf5-devel \
    netcdf \

RUN git clone https://github.com/KNMI/adaguc-services


