warn "Warning: OpenSSL ASN1/PKey/X509/Netscape/PKCS7 implementation unavailable"
warn "You need to download or install BouncyCastle jars (bc-prov-*.jar, bc-mail-*.jar)"
warn "to fix this."
module OpenSSL
  module ASN1
    class ASN1Error < OpenSSLError; end
    class ASN1Data; end
    class Primitive; end
    class Constructive; end
  end
  module PKey
    class PKeyError < OpenSSLError; end
    class PKey; def initialize(*args); end; end
    class RSA < PKey; end
    class DSA < PKey; end
    class DH < PKey; end
  end
  module X509
    class Name; end
    class Certificate; end
    class Extension; end
    class CRL; end
    class Revoked; end
    class Store; end
    class Request; end
    class Attribute; end
  end
  module Netscape
    class SPKI; end
  end
  module PKCS7
    class PKCS7; end
  end
end