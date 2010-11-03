# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{jruby-openssl}
  s.version = "0.5.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ola Bini and JRuby contributors"]
  s.date = %q{2009-06-15}
  s.description = %q{= JRuby-OpenSSL}
  s.email = %q{ola.bini@gmail.com}
  s.extra_rdoc_files = ["History.txt", "README.txt", "License.txt"]
  s.files = ["History.txt", "README.txt", "License.txt", "lib/jopenssl.jar", "lib/bcmail-jdk14-139.jar", "lib/bcprov-jdk14-139.jar", "lib/openssl.rb", "lib/jopenssl/version.rb", "lib/openssl/bn.rb", "lib/openssl/buffering.rb", "lib/openssl/cipher.rb", "lib/openssl/digest.rb", "lib/openssl/dummy.rb", "lib/openssl/dummyssl.rb", "lib/openssl/ssl.rb", "lib/openssl/x509.rb", "test/pkcs7_mime_enveloped.message", "test/pkcs7_mime_signed.message", "test/pkcs7_multipart_signed.message", "test/test_cipher.rb", "test/test_integration.rb", "test/test_java.rb", "test/test_java_attribute.rb", "test/test_java_bio.rb", "test/test_java_mime.rb", "test/test_java_pkcs7.rb", "test/test_java_smime.rb", "test/test_openssl.rb", "test/test_openssl_x509.rb", "test/test_pkey.rb", "test/ut_eof.rb", "test/fixture/cacert.pem", "test/fixture/cert_localhost.pem", "test/fixture/localhost_keypair.pem", "test/openssl/ssl_server.rb", "test/openssl/test_asn1.rb", "test/openssl/test_cipher.rb", "test/openssl/test_digest.rb", "test/openssl/test_hmac.rb", "test/openssl/test_ns_spki.rb", "test/openssl/test_pair.rb", "test/openssl/test_pkcs7.rb", "test/openssl/test_pkey_rsa.rb", "test/openssl/test_ssl.rb", "test/openssl/test_x509cert.rb", "test/openssl/test_x509crl.rb", "test/openssl/test_x509ext.rb", "test/openssl/test_x509name.rb", "test/openssl/test_x509req.rb", "test/openssl/test_x509store.rb", "test/openssl/utils.rb", "test/ref/a.out", "test/ref/compile.rb", "test/ref/pkcs1", "test/ref/pkcs1.c"]
  s.homepage = %q{http://jruby-extras.rubyforge.org/jruby-openssl}
  s.rdoc_options = ["--main", "README.txt"]
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{jruby-extras}
  s.rubygems_version = %q{1.3.4}
  s.summary = %q{OpenSSL add-on for JRuby}
  s.test_files = ["test/test_cipher.rb", "test/test_integration.rb", "test/test_java.rb", "test/test_java_attribute.rb", "test/test_java_bio.rb", "test/test_java_mime.rb", "test/test_java_pkcs7.rb", "test/test_java_smime.rb", "test/test_openssl.rb", "test/test_openssl_x509.rb", "test/test_pkey.rb"]

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 3

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
