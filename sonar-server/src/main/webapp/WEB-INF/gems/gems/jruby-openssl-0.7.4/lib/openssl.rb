=begin
= $RCSfile$ -- Loader for all OpenSSL C-space and Ruby-space definitions

= Info
  'OpenSSL for Ruby 2' project
  Copyright (C) 2002  Michal Rokos <m.rokos@sh.cvut.cz>
  All rights reserved.

= Licence
  This program is licenced under the same licence as Ruby.
  (See the file 'LICENCE'.)

= Version
  $Id: openssl.rb 12496 2007-06-08 15:02:04Z technorama $
=end

# TODO: remove this chunk after 1.4 support is dropped
require 'digest'
unless defined?(::Digest::Class)
  # restricted support for jruby <= 1.4 (1.8.6 Digest compat)
  module Digest
    class Class
      def self.hexdigest(name, data)
        digest(name, data).unpack('H*')[0]
      end

      def self.digest(data, name)
        digester = const_get(name).new
        digester.update(data)
        digester.finish
      end

      def hexdigest
        digest.unpack('H*')[0]
      end

      def digest
        dup.finish
      end

      def ==(oth)
        digest == oth.digest
      end

      def to_s
        hexdigest
      end

      def size
        digest_length
      end

      def length
        digest_length
      end
    end
  end
end
# end of compat chunk.

begin
  require 'bouncy-castle-java'
rescue LoadError
  # runs under restricted mode.
end
require 'jopenssl'


require 'openssl/bn'
require 'openssl/cipher'
require 'openssl/config'
require 'openssl/digest'
require 'openssl/pkcs7'
require 'openssl/ssl'
require 'openssl/x509'

