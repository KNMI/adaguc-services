# Docker setup documentation

The docker container in this repo has the following volumes

- `/data` , for all data, will be filled by the service while it is running
- `/config`, for the configuration, you will have to fill this. see docker/config-template for example versionf of files when available.

## Config file overview and how to get these

### Certificate

A certificate is needed for the secure (https) connectivity. This needs to then be put into a so-called keystore so tomcat can read the certificate. Simplest option is to generate a keystore with a self-signed certificate:

```sh
    #host.name.com = machine you will be using to host the service. Should be a valid dns entry.
    keytool -genkey -noprompt -keypass password -alias tomcat -keyalg RSA -storepass password -keystore keystore.jks -dname CN=host.domain.com
```

Alternatively, you can put an existing certificate in a keystore:

    command-goes-here.sh

Or, use letsencrypt

    letsencrypt-something.sh

### Truststore

adaguc-services needs to know which services to trust, for instance when connecting to an external opendap host. This similair to the root-certificates found in every web brower. If needed you can add certificates to the trust store, for instance if you generated a self-signed certificate somewhere.

Simplest option is to download the truststore useb by all esgf nodes:

```sh
curl -L https://raw.githubusercontent.com/ESGF/esgf-dist/master/installer/certs/esg-truststore.ts > esg-truststore.ts
```

If you need to, you can add certificates to the truststore. Here is an example of how to add a key, where the key is first extracted from a keystore (e.g. if you are using a self-signed certificate).

```sh
# export certificate from a keystore to a file called some-certificate.pem
keytool  -export -alias tomcat -rfc -file ~/some-certificate.pem -keystore keystore.jks -storepass password

# put this certificate from some-certificate.pem into the truststore
keytool -import -v -trustcacerts -alias adagucservicescert -file ~/some-certificate.pem -keystore truststore.ts -storepass changeit -noprompt
```

If you already have a certificate in the truststore and would like to replace it first delete it:
```sh
keytool -delete -alias adagucservicescert  -keystore /esg-truststore.ts -storepass changeit -noprompt
```


### Server Config File

ADAGUC-services has a single main config file. See docker/adaguc-services-config.xml for an example suitable for usage in a docker container. The hostname at the top of the file always needs to be changed to the machine running the service (and be equal to the hostname in the certificate). Point to the config file by setting the `ADAGUC_SERVICES_CONFIG` environment variable.
