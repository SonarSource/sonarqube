warn "OpenSSL ASN1/PKey/X509/Netscape/PKCS7 implementation unavailable"
warn "gem install bouncy-castle-java for full support."
module OpenSSL
  module ASN1
    class ASN1Error < OpenSSLError; end
    class ASN1Data; end
    class Primitive; end
    class Constructive; end
  end
  module X509
    class Name; end
    class Certificate; end
    class Extension; end
    class CRL; end
    class Revoked; end
    class Store
      def set_default_paths; end
    end
    class Request; end
    class Attribute; end
  end
  module Netscape
    class SPKI; end
  end
  class PKCS7
    # this definition causes TypeError "superclass mismatch for class PKCS7"
    # MRI also crashes following definition;
    #   class Foo; class Foo < Foo; end; end
    #   class Foo; class Foo < Foo; end; end
    #
    # class PKCS7 < PKCS7; end
  end
end
