package org.sonar.auth.saml;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.sonar.api.server.ServerSide;

@ServerSide
class SamlCertificateConverter {

  X509Certificate toX509Certificate(String certificateString) {
    String cleanedCertificateString = sanitizeCertificateString(certificateString);

    byte[] decoded = Base64.getDecoder().decode(cleanedCertificateString);
    try {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  private static String sanitizeCertificateString(String certificateString) {
    return certificateString
      .replace("-----BEGIN CERTIFICATE-----", "")
      .replace("-----END CERTIFICATE-----", "")
      .replaceAll("\\s+", "");
  }
}
