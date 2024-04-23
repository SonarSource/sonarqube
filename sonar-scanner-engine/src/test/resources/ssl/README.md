## Let's create TLS certificates

The most common format of certificates are PEM, so let's generate them instead of using Java keytool (that can also generate keys in JKS format).

This README, is a *simplified* version for generating the certificates only for test's purposes.

**DO NOT USE IT FOR PRODUCTION**

### Generation of a TLS server certificate

In this example the configuration of OpenSSL is entirely in openssl.conf (a stripped version of openssl.cnf that may vary from distribution to distribution)

#### First let's create a Certificate Authority

The Certificate Authority is a private key that is used to sign other X.509 certificates in order to validate the ownership of a website (trusted tier).

```bash
$ openssl genrsa -out ca.key 4096
.....++
................................................................................................................................................++
e is 65537 (0x010001)
```

Now we have our key to sign other certificates : `ca.key` in PEM format.

Let's create our X.509 CA certificate :

```bash
$ openssl req -key ca.key -new -x509 -days 3650 -sha256 -extensions ca_extensions -out ca.crt -config ./openssl.conf
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2-letter code) [CH]:
State or Province Name (full name) [Geneva]:
Locality (e.g. city name) [Geneva]:
Organization (e.g. company name) [SonarSource SA]:
Common Name (your.domain.com) [localhost]:
```

There is no important values here.

#### Let's create a self-signed certificate our TLS server using our CA

We want to create a X.509 certificate for our https server. This certificate will be a Certificate Signing Request. A certificate that need to be signed by a trusted tier.
The default configuration is set in `openssl.conf` and it has been configuration for `localhost`.
The most important part is the `Common Name` and `DNS.1` (set in `openssl.conf`).

So just keep using enter with this command line :

```bash
$ openssl req -new -keyout server.key -out server.csr -nodes -newkey rsa:4096 -config ./openssl.conf
  Generating a 4096 bit RSA private key
  ........................................................................++
  .........................................................................................++
  writing new private key to 'server.key'
  -----
  You are about to be asked to enter information that will be incorporated
  into your certificate request.
  What you are about to enter is what is called a Distinguished Name or a DN.
  There are quite a few fields but you can leave some blank
  For some fields there will be a default value,
  If you enter '.', the field will be left blank.
  -----
  Country Name (2-letter code) [CH]:
  State or Province Name (full name) [Geneva]:
  Locality (e.g. city name) [Geneva]:
  Organization (e.g. company name) [SonarSource SA]:
  Common Name (your.domain.com) [localhost]:
```

No we have `server.csr` file valid for 10 years.
Let's see what's in this certificate :
```bash
$ openssl req -verify -in server.csr -text -noout
verify OK
Certificate Request:
    Data:
        Version: 1 (0x0)
        Subject: C = CH, ST = Geneva, L = Geneva, O = SonarSource SA, CN = localhost
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                RSA Public-Key: (4096 bit)
                Modulus:
                    00:c8:2d:dc:64:1a:b6:d9:a9:3e:bd:3f:d3:ae:27:
                    ab:00:a8:09:f7:9e:ae:b5:70:c0:11:ab:2d:45:48:
                    6c:b9:b3:b1:4b:42:b7:4e:48:d3:2e:38:cb:e5:7d:
                    14:30:d3:b8:1d:2f:e2:09:04:cc:aa:80:09:51:bc:
                    59:9d:a7:7a:76:34:cc:7a:2b:ae:d3:ef:98:38:ef:
                    b2:8a:0e:e9:2f:79:4e:d4:a9:10:63:2b:5b:05:05:
                    ef:6b:98:41:e3:c0:3e:6c:5f:8a:66:10:ca:98:e5:
                    37:c6:ea:13:48:c9:92:22:53:44:1a:61:27:f4:60:
                    16:a7:a9:87:a9:d3:cf:88:5e:d4:47:44:24:4f:6d:
                    5e:c0:4a:ff:ad:e4:82:63:da:82:eb:9e:b3:76:6f:
                    5d:b4:2d:fc:96:4a:98:e4:f5:20:97:48:38:11:29:
                    33:7d:5a:96:fa:28:49:9f:cb:24:f8:02:f6:bb:ed:
                    f3:91:90:51:10:c2:93:28:56:6e:4d:51:51:10:27:
                    8f:c3:f0:cd:ee:51:2d:dc:e5:a7:21:55:20:44:ac:
                    8b:66:1d:b7:eb:e0:ed:69:f0:d4:32:82:ee:53:91:
                    3b:ee:58:83:ba:3b:9d:3f:f7:23:0e:36:46:20:6b:
                    6a:80:9b:11:46:28:39:60:25:69:9e:e5:d0:34:ba:
                    2b:c3:33:f2:44:3d:fb:8f:2d:47:a6:ae:64:9a:b3:
                    5a:f0:ed:cb:3e:86:33:80:23:32:d0:e7:51:91:a8:
                    c6:97:d1:7c:e4:02:52:5d:7c:a9:97:83:00:c5:10:
                    fb:13:f9:29:1f:79:c4:a5:8c:7b:64:e0:cd:b6:a1:
                    34:36:aa:f4:63:63:77:12:d3:fa:fe:1d:54:2e:64:
                    43:38:a2:71:28:72:7a:bf:33:cb:8c:27:a7:66:51:
                    8f:6f:e8:d2:90:19:2f:d4:8e:ac:b4:7b:e0:53:a8:
                    0f:11:d1:7d:08:71:de:0a:a4:63:10:79:c8:e8:bf:
                    7e:be:8b:06:7d:43:9b:4b:a1:0a:49:a6:c8:c6:43:
                    c4:24:23:13:2a:b2:f9:f2:b8:e7:8e:ab:3e:2a:b5:
                    50:26:23:d6:b2:d3:ee:23:ec:d1:36:92:70:2e:df:
                    82:6a:d2:07:bb:f0:97:51:42:e4:d8:49:69:35:bb:
                    38:90:1f:8e:aa:1d:27:78:26:26:d4:36:75:ee:83:
                    17:69:cb:7f:53:45:8f:b4:63:13:d5:fd:42:10:8a:
                    d3:75:38:4a:bd:13:cf:68:5e:41:6d:f0:57:b5:75:
                    e3:dc:10:82:ab:29:ed:a1:27:9c:50:74:f2:4c:4a:
                    a3:78:2a:53:ca:90:a6:89:20:24:85:b5:ec:c9:c7:
                    be:96:b5
                Exponent: 65537 (0x10001)
        Attributes:
        Requested Extensions:
            X509v3 Subject Alternative Name: 
                DNS:localhost
            X509v3 Key Usage: 
                Digital Signature, Key Encipherment, Data Encipherment
            X509v3 Extended Key Usage: 
                TLS Web Server Authentication
    Signature Algorithm: sha256WithRSAEncryption
         bf:9d:6e:2f:cc:40:9b:92:29:c2:f1:0a:85:6c:35:eb:8e:fa:
         13:0c:53:58:33:5f:7b:09:58:5f:dd:94:7e:2c:65:ed:73:91:
         2a:6b:cc:2d:ec:26:1c:8e:95:57:d9:35:19:82:4f:42:59:81:
         d9:b7:bb:08:70:28:70:35:50:f6:6a:46:e0:2a:ab:90:50:5a:
         dc:b0:c3:b8:52:d7:5c:90:8f:4c:61:09:2c:ba:4a:31:37:6f:
         e0:b9:6b:98:dd:aa:dd:52:66:7e:06:f1:8a:4b:bc:23:0d:62:
         d3:b9:86:8f:3e:cc:05:2b:4d:c4:ad:cf:ae:be:33:22:f6:95:
         00:f0:36:96:26:5e:42:84:d0:2a:79:41:1e:18:10:1c:96:3e:
         9a:8b:cc:a5:f9:59:5b:78:d0:a1:a5:2e:4d:55:30:10:0b:cd:
         13:bc:75:9a:49:e0:de:a4:4d:ed:9b:e8:42:2f:74:2b:dc:6f:
         2d:d3:38:a9:e8:f8:98:2c:56:aa:3e:dd:0d:48:78:16:4c:50:
         fd:0a:b3:3c:28:ac:64:7e:e9:bb:10:0e:3b:29:68:40:a9:19:
         5a:2c:5c:d6:7e:32:39:96:49:a7:4c:6a:a6:09:8e:d4:b8:1e:
         3e:2c:93:c3:2c:da:f2:09:20:ef:f4:a9:d2:ff:de:cd:7b:20:
         66:46:ff:c2:36:c3:7d:32:d6:55:d1:fe:0f:00:9a:23:56:97:
         52:a1:0a:52:64:29:50:c7:5d:b4:1e:e4:67:9a:07:3f:fb:85:
         03:00:22:d8:f5:e6:bc:95:bf:bc:08:ab:4d:32:4c:d6:52:e0:
         72:3e:8a:a5:85:72:43:d6:d4:51:6e:99:9a:1f:d8:0e:fd:4d:
         59:81:7e:c1:81:6d:3b:69:76:ce:53:a4:c0:69:46:72:b2:fe:
         40:b3:a5:5c:b0:ce:d2:61:83:be:0f:c3:85:a0:21:a7:e8:fd:
         2f:2c:1c:68:24:1d:9b:a3:43:cb:5e:30:21:af:e8:2e:4e:ec:
         ea:a7:d2:68:f1:bd:3f:3c:41:48:ac:91:f9:9d:e8:f2:3d:cb:
         d0:82:d2:00:ed:7b:fa:d8:98:e3:a8:74:f2:ce:70:95:0a:9d:
         c2:b2:cc:08:d1:fd:de:26:d3:3e:c0:62:28:9b:b4:2d:f4:b5:
         6d:48:c9:d3:05:f5:1e:68:17:6b:fb:02:2e:20:98:1a:de:d4:
         ae:6b:e0:68:97:98:e0:4f:47:ec:14:fd:dc:57:d2:e2:5c:59:
         36:a5:0b:94:b7:4e:b8:ae:ee:c9:ac:02:ae:43:bf:9f:07:da:
         0c:44:b0:47:69:1d:64:ea:bd:68:af:4f:a7:9a:1f:b1:b9:1d:
         71:0e:86:4e:0c:ff:a3:1d
```

#### Let's sign this certificate with our own CA

The CSR will be signed with our previously created ca.key
We'll sign it to be valid for 10years (3650)

```bash
$ openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.pem -sha256 -extfile v3.ext
Signature ok
subject=C = CH, ST = Geneva, L = Geneva, O = SonarSource SA, CN = localhost
Getting CA Private Key
```

Let's verify what are in this certificate :

```bash
$ openssl x509 -in server.pem -text -noout
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            d5:c5:2a:c2:c8:f6:43:c7
        Signature Algorithm: sha256WithRSAEncryption
        Issuer: C = CH, ST = Geneva, L = Geneva, O = SonarSource SA, CN = SonarSource SA
        Validity
            Not Before: Mar 17 14:12:29 2020 GMT
            Not After : Mar 15 14:12:29 2030 GMT
        Subject: C = CH, ST = Geneva, L = Geneva, O = SonarSource SA, CN = localhost
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                RSA Public-Key: (4096 bit)
                Modulus:
                    00:a2:43:1e:8b:60:b5:e0:61:3e:99:a4:54:93:c8:
                    16:14:c2:fa:fd:e5:7c:05:02:71:09:46:d9:2a:52:
                    57:12:d7:74:46:6a:bd:d4:de:4a:06:b2:51:83:2c:
                    98:07:8c:b0:f7:e1:8a:aa:fc:0c:30:c6:d7:ec:57:
                    0b:a7:12:45:e3:13:1a:26:e8:22:d8:fd:2a:9e:ae:
                    7b:20:b8:41:99:50:0e:b7:1c:bb:78:18:60:25:67:
                    78:5b:af:d8:7f:d1:01:12:81:0a:1f:dd:f0:54:bc:
                    57:16:05:22:7c:65:a2:7e:03:ed:e8:7f:50:b1:cd:
                    7c:e8:7b:58:cb:df:6d:e3:04:03:78:a4:83:e7:20:
                    c4:37:bc:00:ba:7c:12:d9:ac:52:88:88:72:df:fc:
                    35:8f:94:f0:1b:33:f8:94:b8:bc:ab:0e:89:68:5f:
                    92:1b:af:c9:da:c2:c2:e2:a1:c3:8e:c8:16:1a:9e:
                    89:7a:b4:24:2c:24:df:c5:26:59:ab:d8:f9:06:39:
                    02:c0:0d:88:5a:0c:14:e7:bc:c5:b8:4c:e5:e0:85:
                    b2:0b:88:36:b3:d5:35:10:e9:b8:5a:48:69:1a:b3:
                    2a:4a:d6:f3:f5:6a:91:41:f8:1e:da:d0:0e:21:c3:
                    a2:f8:5c:08:42:a2:2b:13:be:63:e5:67:d5:19:2f:
                    2c:96:6d:17:1c:7f:34:19:68:cf:91:b6:14:d9:9a:
                    1b:1c:f9:08:d7:f9:2d:c3:48:14:3d:02:d4:90:f7:
                    f2:74:65:f8:22:2d:46:b2:76:cd:46:c1:8e:ab:a1:
                    11:d7:12:14:77:e3:1c:c3:1c:fa:32:79:0e:0e:59:
                    55:e4:9d:60:d7:18:0b:25:82:97:28:30:df:de:89:
                    5b:56:37:a2:33:86:26:12:83:75:f0:02:ae:88:b5:
                    d6:5e:a2:b7:e7:57:9d:de:72:ad:d6:55:2a:e1:a8:
                    4c:15:18:a9:e3:22:52:f1:74:e1:b0:d2:e7:9b:ec:
                    f9:6d:5f:86:c2:9c:e2:22:f2:f4:11:a2:d1:71:b8:
                    77:e4:8c:4c:ed:84:e8:f9:82:a2:f1:73:95:19:08:
                    92:d5:b3:50:be:bc:c2:ec:0e:d7:da:53:d2:22:36:
                    c8:d8:48:d1:22:0d:42:a7:68:6d:e5:b6:5f:00:7d:
                    70:e4:5f:fe:df:db:3a:96:30:c8:76:89:e9:d1:98:
                    1e:63:e2:d0:29:46:b0:3d:f6:38:d7:07:40:47:0e:
                    a3:a5:70:1c:8b:80:c1:81:d1:35:cd:3d:93:20:c6:
                    7c:10:a4:09:ed:41:12:2e:c3:66:e5:47:96:58:de:
                    53:1b:d5:67:2c:1d:55:3b:c1:03:28:cf:5e:aa:33:
                    2b:8c:e1
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            X509v3 Authority Key Identifier: 
                keyid:26:4F:F6:F9:E6:8B:B6:F7:59:CE:30:23:5C:90:2E:AE:7A:20:C4:DB

            X509v3 Basic Constraints: 
                CA:FALSE
            X509v3 Key Usage: 
                Digital Signature, Non Repudiation, Key Encipherment, Data Encipherment
            X509v3 Subject Alternative Name: 
                DNS:localhost
    Signature Algorithm: sha256WithRSAEncryption
         b0:df:99:da:44:e1:22:c6:51:da:e1:b5:a9:fd:fe:82:d6:74:
         07:ad:d4:b4:f8:29:3e:57:7a:1b:98:36:4e:0a:23:68:f5:27:
         c7:52:59:90:cd:94:23:08:83:6f:a4:af:14:a3:e3:ed:f2:13:
         e4:17:f7:7c:27:45:bc:8c:9a:1d:f3:90:c6:b4:3e:e8:7a:c1:
         18:e4:8e:8c:28:ac:02:c7:d1:4c:e3:67:7a:13:69:ff:a4:74:
         c4:82:d7:54:d3:cb:7b:4e:f9:25:36:90:33:43:f0:b8:a5:e6:
         7c:ea:3d:41:fe:51:3c:bc:d2:c6:4e:9c:dc:04:69:23:08:70:
         bf:69:2a:bd:28:8c:3f:a1:f0:b0:88:87:a2:af:63:85:86:e3:
         07:2a:74:89:d0:69:b3:8c:7d:a5:db:ec:f2:5c:56:33:89:04:
         c6:75:a9:a2:b8:c0:1b:b5:dd:0f:96:50:71:ad:39:36:39:13:
         d0:80:f3:c8:50:db:d2:65:4d:56:75:9c:70:c2:d6:0c:6b:4a:
         6e:f7:f1:76:1b:82:16:13:eb:37:4f:05:fd:8f:06:89:15:d7:
         6d:a7:4e:43:bb:ee:b1:a8:c0:f4:cd:d7:1f:17:c3:3f:1a:79:
         8f:6e:46:a4:e5:1f:82:8d:60:6f:6c:a2:f4:9b:6e:59:85:48:
         73:ae:78:dd:c1:fa:81:1f:38:56:84:fc:31:98:af:a8:e4:bf:
         62:45:16:38:4a:5d:0e:6a:c4:bf:e1:9b:2b:c4:eb:dc:d4:85:
         82:0f:6c:31:54:1c:46:62:51:22:c3:0d:e4:ca:2e:c9:5f:f5:
         8c:7a:8c:c2:1d:f2:a8:f9:65:e6:ca:4e:6d:21:4e:55:07:6c:
         58:0d:fd:59:76:9c:65:7f:26:8f:8b:7b:01:70:5f:59:25:66:
         a8:9b:0a:70:a1:d8:fd:61:26:7e:4d:5f:3c:28:74:2b:94:fb:
         2a:8e:35:51:77:5a:96:a9:9b:4e:18:b6:6d:0b:55:4e:2e:15:
         ca:e7:cb:15:29:0e:b9:fd:23:56:a7:ad:dc:a1:b9:1b:1b:19:
         24:10:e3:a5:cb:69:2b:40:74:3c:3e:31:ac:a9:0d:17:6b:51:
         61:d4:5e:d1:98:b6:81:29:55:92:1f:00:8d:4c:72:d4:3a:0e:
         fd:1f:30:73:04:b8:99:6f:27:57:9a:6c:2b:e1:fa:c2:d3:bf:
         d3:d2:24:f3:5c:30:a3:25:d6:f5:18:91:13:d4:55:1e:33:89:
         b7:99:27:a9:14:e4:d9:32:50:ba:56:2f:53:b7:a1:d7:d3:14:
         2f:e2:73:5a:d4:b2:94:73:14:ef:ac:6f:a1:c1:84:31:17:fd:
         fa:f8:62:d3:eb:a5:8a:34
```

#### Let's create a PKCS12 file to be used for starting a TLS server

```bash
$ openssl pkcs12 -export -in server.pem -inkey server.key -name localhost -out server.p12
Enter Export Password: pwdServerP12
Verifying - Enter Export Password: pwdServerP12
```

The password of the PKCS12 file is `pwdServerP12`

The `server.p12` file can now be used to start a TLS server.

#### Now we'll generate the `client-truststore.p12` file that will have the server CA certificate. 
Since we don't need to add the key of the certificate (only required to sign, not to verify), we can import it directly with keytool.

```bash
$ keytool -import -trustcacerts -alias server-ca -keystore client-truststore.p12 -file ca.crt
Enter keystore password: pwdClientWithServerCA 
Re-enter new password: pwdClientWithServerCA
Owner: CN=SonarSource, O=SonarSource SA, L=Geneva, ST=Geneva, C=CH
Issuer: CN=SonarSource, O=SonarSource SA, L=Geneva, ST=Geneva, C=CH
Serial number: ed8bcadd4888ffac
Valid from: Sat Sep 15 08:10:22 CEST 2018 until: Tue Sep 12 08:10:22 CEST 2028
Certificate fingerprints:
	 MD5:  25:38:06:14:D0:B3:36:81:65:FC:44:CA:E3:BA:57:12
	 SHA1: 77:56:EF:C7:2F:5A:29:D1:A0:54:5F:F8:B4:19:60:91:7B:71:E4:2C
	 SHA256: 1D:2D:E5:52:21:60:75:08:F3:0A:B3:93:CF:38:F6:30:88:56:28:73:20:BA:76:9A:C0:A1:D7:8C:4D:D3:84:AA
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 4096-bit RSA key
Version: 3

Extensions: 

#1: ObjectId: 2.5.29.35 Criticality=false
AuthorityKeyIdentifier [
KeyIdentifier [
0000: 87 B9 C1 23 E2 F1 A3 68   BD D6 44 99 0E AD FC FC  ...#...h..D.....
0010: A5 31 90 D4                                        .1..
]
]

#2: ObjectId: 2.5.29.19 Criticality=true
BasicConstraints:[
  CA:true
  PathLen:2147483647
]

#3: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
]

#4: ObjectId: 2.5.29.15 Criticality=false
KeyUsage [
  DigitalSignature
  Key_Encipherment
  Data_Encipherment
  Key_CertSign
  Crl_Sign
]

#5: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 87 B9 C1 23 E2 F1 A3 68   BD D6 44 99 0E AD FC FC  ...#...h..D.....
0010: A5 31 90 D4                                        .1..
]
]

Trust this certificate? [no]:  yes
Certificate was added to keystore
```

### Create a certificate that will be used to authenticate a user

The principle is the same we'll have a CA authority signing certificates that will be sent by the user to the server.
In this case the server will have to host the CA authority in its TrustedKeyStore while the client will host his certificate in is KeyStore.
In this use case, the extensions are not the same, so we'll use openssl-client-auth.conf

#### Generation of CA

One line to generate both the key `ca-lient-auth.key` and the CA certificate `ca-client-auth.crt`

```bash
openssl req -newkey rsa:4096 -nodes -keyout ca-client-auth.key -new -x509 -days 3650 -sha256 -extensions ca_extensions -out ca-client-auth.crt -subj '/C=CH/ST=Geneva/L=Geneva/O=SonarSource SA/CN=SonarSource/' -config ./openssl-client-auth.conf
Generating a 4096 bit RSA private key
...................................++
............................................................................................................................................................................................................................................................++
writing new private key to 'ca-client-auth.key'
-----

```

For the certificate, the Common Name is used to identify the user
```bash
$ openssl req -new -keyout client.key -out client.csr -nodes -newkey rsa:4096 -subj '/C=CH/ST=Geneva/L=Geneva/O=SonarSource SA/CN=Julien Henry/' -config ./openssl-client-auth.conf
Generating a 4096 bit RSA private key
..............................................++
................++
writing new private key to 'client.key'
-----
```

Let's sign this certificate
```bash
$ openssl x509 -req -days 3650 -in client.csr -CA ca-client-auth.crt -CAkey ca-client-auth.key -CAcreateserial -out client.pem -sha256
Signature ok
subject=C = CH, ST = Geneva, L = Geneva, O = SonarSource SA, CN = Julien Henry
Getting CA Private Key
```

Let's create the pkcs12 store containing the client certificate

```bash
$ openssl pkcs12 -export -in client.pem -inkey client.key -name julienhenry -out client.p12
Enter Export Password: pwdClientCertP12
Verifying - Enter Export Password: pwdClientCertP12
```

This will go to client keyStore.
Now we'll generate the `server-with-client-ca.p12` file that will have the CA certificate. Since we don't need to add the key of the certificate (only required to sign, not to verify), we can import it directly with keytool.

```bash
$ keytool -import -trustcacerts -alias client-ca -keystore server-with-client-ca.p12 -file ca-client-auth.crt
Enter keystore password: pwdServerWithClientCA 
Re-enter new password: pwdServerWithClientCA
Owner: CN=SonarSource, O=SonarSource SA, L=Geneva, ST=Geneva, C=CH
Issuer: CN=SonarSource, O=SonarSource SA, L=Geneva, ST=Geneva, C=CH
Serial number: ed8bcadd4888ffac
Valid from: Sat Sep 15 08:10:22 CEST 2018 until: Tue Sep 12 08:10:22 CEST 2028
Certificate fingerprints:
	 MD5:  25:38:06:14:D0:B3:36:81:65:FC:44:CA:E3:BA:57:12
	 SHA1: 77:56:EF:C7:2F:5A:29:D1:A0:54:5F:F8:B4:19:60:91:7B:71:E4:2C
	 SHA256: 1D:2D:E5:52:21:60:75:08:F3:0A:B3:93:CF:38:F6:30:88:56:28:73:20:BA:76:9A:C0:A1:D7:8C:4D:D3:84:AA
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 4096-bit RSA key
Version: 3

Extensions: 

#1: ObjectId: 2.5.29.35 Criticality=false
AuthorityKeyIdentifier [
KeyIdentifier [
0000: 87 B9 C1 23 E2 F1 A3 68   BD D6 44 99 0E AD FC FC  ...#...h..D.....
0010: A5 31 90 D4                                        .1..
]
]

#2: ObjectId: 2.5.29.19 Criticality=true
BasicConstraints:[
  CA:true
  PathLen:2147483647
]

#3: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
]

#4: ObjectId: 2.5.29.15 Criticality=false
KeyUsage [
  DigitalSignature
  Key_Encipherment
  Data_Encipherment
  Key_CertSign
  Crl_Sign
]

#5: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 87 B9 C1 23 E2 F1 A3 68   BD D6 44 99 0E AD FC FC  ...#...h..D.....
0010: A5 31 90 D4                                        .1..
]
]

Trust this certificate? [no]:  yes
Certificate was added to keystore

```