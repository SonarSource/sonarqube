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

import com.google.common.annotations.VisibleForTesting;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

public class SonarqubeRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

  private static final String ANY_URL = "http://anyurl";

  private final SamlSettings samlSettings;
  private final SamlCertificateConverter samlCertificateConverter;
  private final SamlPrivateKeyConverter samlPrivateKeyConverter;

  @CheckForNull
  private final String callbackUrl;

  SonarqubeRelyingPartyRegistrationRepository(SamlSettings samlSettings, SamlCertificateConverter samlCertificateConverter, SamlPrivateKeyConverter samlPrivateKeyConverter,
    @Nullable String callbackUrl) {
    this.samlSettings = samlSettings;
    this.samlCertificateConverter = samlCertificateConverter;
    this.samlPrivateKeyConverter = samlPrivateKeyConverter;
    this.callbackUrl = callbackUrl;
  }

  @Override
  public RelyingPartyRegistration findByRegistrationId(String registrationId) {
    X509Certificate x509Certificate = samlCertificateConverter.toX509Certificate(samlSettings.getCertificate());
    RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId("saml")
      .assertionConsumerServiceLocation(callbackUrl != null ? callbackUrl : ANY_URL)
      .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
      .entityId(samlSettings.getApplicationId())
      .assertingPartyMetadata(metadata -> metadata
        .entityId(samlSettings.getProviderId())
        .singleSignOnServiceLocation(validateLoginUrl(samlSettings.getLoginUrl()))
        .verificationX509Credentials(c -> c.add(Saml2X509Credential.verification(x509Certificate)))
        .wantAuthnRequestsSigned(samlSettings.isSignRequestsEnabled())
      );
    addSignRequestFieldsIfNecessary(builder);
    return builder.build();
  }

  private static String validateLoginUrl(String url) {
    try {
      return new URL(url).toURI().toString();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Invalid SAML Login URL", e);
    }
  }

  private void addSignRequestFieldsIfNecessary(RelyingPartyRegistration.Builder builder) {
    //(on SQ) to sign request we need SP private key and certificate
    //(on IDP) to verify request IDP needs SP public key (certificate)

    //(on IDP) to sign response we need IDP private key (embedded)
    //(on SQ) to verify response we need IDP public key (certificate) !mandatory!

    //(on IDP) encryption: we need SP public key (certificate)
    //(on SQ) decryption: we need Service Provide private key and certificate
    Optional<String> serviceProviderPrivateKey = samlSettings.getServiceProviderPrivateKey();

    if (serviceProviderPrivateKey.isEmpty() || samlSettings.getServiceProviderCertificate() == null) {
      if (samlSettings.isSignRequestsEnabled()) {
        throw new IllegalStateException("Sign requests is enabled but SonarQube private key and/or SonarQube certificate is missing");
      }
      return;
    }

    String privateKeyString = serviceProviderPrivateKey.get();
    String serviceProviderCertificateString = samlSettings.getServiceProviderCertificate();
    PrivateKey privateKey = samlPrivateKeyConverter.toPrivateKey(privateKeyString);
    X509Certificate spX509Certificate = samlCertificateConverter.toX509Certificate(serviceProviderCertificateString);
    builder.decryptionX509Credentials(c -> c.add(Saml2X509Credential.decryption(privateKey, spX509Certificate)));

    if (samlSettings.isSignRequestsEnabled()) {
      builder.signingX509Credentials(c -> c.add(Saml2X509Credential.signing(privateKey, spX509Certificate)));
    }
  }

  @VisibleForTesting
  SamlSettings getSamlSettings() {
    return samlSettings;
  }

  @VisibleForTesting
  String getCallbackUrl() {
    return callbackUrl;
  }
}
