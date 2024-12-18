package org.sonar.auth.saml;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.sonar.api.server.ServerSide;

@ServerSide
class SamlPrivateKeyConverter {
  PrivateKey toPrivateKey(String privateKeyString) {
    String cleanedPrivateKeyString = sanitizePrivateKeyString(privateKeyString);

    byte[] decoded = Base64.getDecoder().decode(cleanedPrivateKeyString);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  private static String sanitizePrivateKeyString(String privateKeyString) {
    return privateKeyString
      .replace("-----BEGIN PRIVATE KEY-----", "")
      .replace("-----END PRIVATE KEY-----", "")
      .replaceAll("\\s+", "");
  }
}
