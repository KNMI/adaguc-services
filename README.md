# adaguc-services
Services for adaguc-server

For setting up development environment:

1) Download and install spring tool suite (https://spring.io/tools/sts/all)
2) Download lombok.jar (https://projectlombok.org/download.html)
3) Install lombok into spring tool suite with java -jar lombok.jar
3) Start STS and import this project as existing project
4) Press alt F5 to update Maven
5) In STS, select Run as java application
6) Select AdagucServicesApplication
7) To adjust the server port, set in Run Configuration the argument like this: --server.port=8090
8) Copy pre-commit to ./git/hooks to enable automatic unit testing on new commits.

For setting up your adaguc and wps server:
1) Create the following directory structure:
   adagucservices_dir
    - data
        - adaguc-services-base
            - tokenstore
        - adaguc-services-space
    - config
    - src
        - .globus/certificates
        - adaguc-services-tmp
        - wpsoutputs
    - keystore
2) Download adaguc server into adagucservices_dir/src (for example: git clone https://github.com/KNMI/adaguc-server src/adaguc-server)
3) Run the script compile.sh in the adaguc-server repository
4) Download pywps (for example: curl -L -O https://github.com/geopython/pywps/archive/pywps-3.2.5.tar.gz)
5) Extract pywps in adagucservices_dir/src (for example: cd src && tar xvf ../pywps-3.2.5.tar.gz)
6) Set the environment variable ADAGUCSERVICES_DIR such that it points to the adagucservices_dir
7) Copy adaguc-services-config.xml.example to your home directory as adaguc-services-config.xml

For setting up certificates for your development environment:
1) Generate a certificate inside adagucservices_dir:
${JAVA_HOME}/bin/keytool -genkey -noprompt -keypass password -alias tomcat -keyalg RSA -storepass password -keystore ./keystore/c4i_keystore.jks  -dname CN=KNMI-data-sciences
2) Copy the certificate to the truststore
cp $JAVA_HOME/jre/lib/security/cacerts config/ds-truststore.ts
3) The private key needs to be kept secure, it is used by the CA to create new CA files: the file is knmi_ds_rootca.key:
openssl genrsa -out config/knmi_ds_rootca.key 2048
4) The CA file is created for a special context, in this case: knmi_ds_ca.pem
openssl req -x509 -days 3650 -new -nodes -key config/knmi_ds_rootca.key -sha256 -out config/knmi_ds_ca.pem -subj '/O=KNMI/OU=RDWDT/CN=knmi_datasciences_ca_tokenapi'
5) In case of a tomcat server it needs to be added to the truststore: (same as certificates for apache http server):
${JAVA_HOME}/bin/keytool -import -v -trustcacerts -alias knmi_ds_ca.pem -file config/knmi_ds_ca.pem -keystore config/ds-truststore.ts  -storepass changeit -noprompt
6) User: Create privatekey and Certificate Signing Request:
openssl genrsa -des3 -out config/${USER}.key 2048  -subj "/O=KNMI/OU=RDWD/CN=${USER}"
openssl rsa -in config/${USER}.key -out config/${USER}nopass.key
7) CSR Signing request:
openssl req -new -key config/${USER}nopass.key -out config/${USER}.csr -subj "/O=KNMI/OU=RDWD/CN=${USER}"
HOST=`hostname`
echo | openssl s_client -connect ${HOST}:8090 2>&1 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p'  > config/adagucservices.pem
8) Admin: CA now signs the CSR and sends ${USER}.crt file to User
openssl x509 -req -in config/${USER}.csr  -CA config/knmi_ds_ca.pem  -CAkey config/knmi_ds_rootca.key -CAcreateserial -out config/${USER}.crt -days 3600
9) Create a .p12 file which needs to be imported in your browser:
openssl pkcs12 -export -clcerts -in config/${USER}.crt -inkey config/${USER}nopass.key -out config/${USER}.p12

For creating a new package:

1) Adjust the version in pom.xml: 0.<sprint number>.version (this is named ${VERSION} from now on)
2) Type mvn package
3) in directory target the file ./target/demo-${VERSION}-SNAPSHOT.jar is created.
4) You can for example start this with java -jar demo-${VERSION}-SNAPSHOT.jar




