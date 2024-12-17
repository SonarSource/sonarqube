/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.auth.saml;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

public class SonarqubeRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

  private static final String ANY_URL = "http://anyurl";

  private final SamlSettings samlSettings;

  @CheckForNull
  private final String callbackUrl;

  public SonarqubeRelyingPartyRegistrationRepository(SamlSettings samlSettings, @Nullable String callbackUrl) {
    this.samlSettings = samlSettings;
    this.callbackUrl = callbackUrl;
  }

  @Override
  public RelyingPartyRegistration findByRegistrationId(String registrationId) {
    RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId("saml")
      .assertionConsumerServiceLocation(callbackUrl != null ? callbackUrl : ANY_URL)
      .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
      .entityId(samlSettings.getApplicationId())
      .assertingPartyMetadata(metadata -> metadata
        .entityId(samlSettings.getProviderId())
        .singleSignOnServiceLocation(samlSettings.getLoginUrl())
        .verificationX509Credentials(c -> c.add(convertStringToSaml2X509Credential(samlSettings.getCertificate())))
        .wantAuthnRequestsSigned(samlSettings.isSignRequestsEnabled())
      );

    if(samlSettings.isSignRequestsEnabled()) {
      builder
        .signingX509Credentials(c -> c.add(convertStringToSaml2X509Credential(samlSettings.getServiceProviderCertificate(),
          samlSettings.getServiceProviderPrivateKey().get(), Saml2X509Credential.Saml2X509CredentialType.SIGNING)))
        .decryptionX509Credentials(c -> c.add(convertStringToSaml2X509Credential(samlSettings.getServiceProviderCertificate(),
          samlSettings.getServiceProviderPrivateKey().get(), Saml2X509Credential.Saml2X509CredentialType.DECRYPTION)));
    }
    return builder.build();
  }

  public Saml2X509Credential convertStringToSaml2X509Credential(String certificateString, String privateKey, Saml2X509Credential.Saml2X509CredentialType type){
    return new Saml2X509Credential(convertStringToPrivateKey(privateKey), getX509Certificate(certificateString), type);
  }

  public Saml2X509Credential convertStringToSaml2X509Credential(String certificateString){
    X509Certificate certificate = getX509Certificate(certificateString);

    // Create and return the Saml2X509Credential
    return Saml2X509Credential.verification(certificate);
  }


  public static PrivateKey convertStringToPrivateKey(String privateKeyString){
    // Remove the "BEGIN" and "END" lines and any whitespace
    String cleanedPrivateKeyString = privateKeyString
      .replace("-----BEGIN PRIVATE KEY-----", "")
      .replace("-----END PRIVATE KEY-----", "")
      .replaceAll("\\s+", "");

    // Decode the base64 encoded string
    byte[] decoded = Base64.getDecoder().decode(cleanedPrivateKeyString);

    // Create a PrivateKey from the decoded bytes
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }


  private static X509Certificate getX509Certificate(String certificateString) {
    String cleanedCertificateString = certificateString
      .replace("-----BEGIN CERTIFICATE-----", "")
      .replace("-----END CERTIFICATE-----", "")
      .replaceAll("\\s+", "");

    // Decode the base64 encoded string
    byte[] decoded = Base64.getDecoder().decode(cleanedCertificateString);

    // Create an X509Certificate from the decoded bytes
    CertificateFactory factory;
    X509Certificate certificate;
    try {
      factory = CertificateFactory.getInstance("X.509");
      certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
    return certificate;
  }
}
