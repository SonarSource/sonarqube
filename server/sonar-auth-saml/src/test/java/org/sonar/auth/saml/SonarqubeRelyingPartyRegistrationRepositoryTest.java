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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SonarqubeRelyingPartyRegistrationRepositoryTest {

  public static final String APPLICATION_ID = "applicationId";
  public static final String PROVIDER_ID = "providerId";
  public static final String SSO_URL = "https://ssoUrl.com";
  public static final String CERTIF_STRING = "certifString";
  private static final String CERTIF_SP_STRING = "certifSpString";
  public static final String PRIVATE_KEY = "privateKey";
  public static final String CALLBACK_URL = "callback/";

  @Mock
  private SamlSettings samlSettings;
  @Mock
  private SamlCertificateConverter samlCertificateConverter;
  @Mock
  private SamlPrivateKeyConverter samlPrivateKeyConverter;

  @Test
  void findByRegistrationId_whenSignRequestsIsDisabledAndAllFieldSet_succeeds() {
    when(samlSettings.getApplicationId()).thenReturn(APPLICATION_ID);
    when(samlSettings.getProviderId()).thenReturn(PROVIDER_ID);
    when(samlSettings.getLoginUrl()).thenReturn(SSO_URL);
    when(samlSettings.getCertificate()).thenReturn(CERTIF_STRING);

    X509Certificate mockedCertificate = mockCertificate();

    RelyingPartyRegistration registration = findRegistration(CALLBACK_URL);

    assertCommonFields(registration, mockedCertificate, CALLBACK_URL);

    assertThat(registration.getAssertingPartyMetadata().getWantAuthnRequestsSigned()).isFalse();
    assertThat(registration.getSigningX509Credentials()).isEmpty();
    assertThat(registration.getDecryptionX509Credentials()).isEmpty();

    verifyNoInteractions(samlPrivateKeyConverter);
  }

  @Test
  void findByRegistrationId_whenCallbackUrlIsNull_succeeds() {
    when(samlSettings.getApplicationId()).thenReturn(APPLICATION_ID);
    when(samlSettings.getProviderId()).thenReturn(PROVIDER_ID);
    when(samlSettings.getLoginUrl()).thenReturn(SSO_URL);
    when(samlSettings.getCertificate()).thenReturn(CERTIF_STRING);

    X509Certificate mockedCertificate = mockCertificate();

    RelyingPartyRegistration registration = findRegistration(null);

    assertCommonFields(registration, mockedCertificate, "http://anyurl");
  }

  @Test
  void findByRegistrationId_whenSignRequestIsEnabledAndAllFieldSet_succeeds() {
    when(samlSettings.getApplicationId()).thenReturn(APPLICATION_ID);
    when(samlSettings.getProviderId()).thenReturn(PROVIDER_ID);
    when(samlSettings.getLoginUrl()).thenReturn(SSO_URL);
    when(samlSettings.getCertificate()).thenReturn(CERTIF_STRING);
    when(samlSettings.isSignRequestsEnabled()).thenReturn(true);
    when(samlSettings.getServiceProviderPrivateKey()).thenReturn(Optional.of(PRIVATE_KEY));
    when(samlSettings.getServiceProviderCertificate()).thenReturn(CERTIF_SP_STRING);

    X509Certificate certificate = mockCertificate();
    X509Certificate serviceProviderCertificate = mockServiceProviderCertificate();
    PrivateKey privateKey = mockPrivateKey();

    RelyingPartyRegistration registration = findRegistration(CALLBACK_URL);

    assertCommonFields(registration, certificate, CALLBACK_URL);

    assertThat(registration.getAssertingPartyMetadata().getWantAuthnRequestsSigned()).isTrue();
    assertThat(registration.getSigningX509Credentials()).containsExactly(Saml2X509Credential.signing(privateKey, serviceProviderCertificate));
    assertThat(registration.getDecryptionX509Credentials()).containsExactly(Saml2X509Credential.decryption(privateKey, serviceProviderCertificate));
  }

  @Test
  void findByRegistrationId_whenSignRequestIsEnabledAndPrivateKeyEmpty_throws() {
    when(samlSettings.getApplicationId()).thenReturn(APPLICATION_ID);
    when(samlSettings.getProviderId()).thenReturn(PROVIDER_ID);
    when(samlSettings.getLoginUrl()).thenReturn(SSO_URL);
    when(samlSettings.getCertificate()).thenReturn(CERTIF_STRING);
    when(samlSettings.isSignRequestsEnabled()).thenReturn(true);
    when(samlSettings.getServiceProviderPrivateKey()).thenReturn(Optional.empty());
    mockCertificate();

    assertThatIllegalStateException()
      .isThrownBy(() -> findRegistration(CALLBACK_URL))
      .withMessage("Sign requests is enabled but private key is missing");
  }

  @Test
  void findByRegistrationId_whenInvalidUrl_throws() {
    when(samlSettings.getApplicationId()).thenReturn(APPLICATION_ID);
    when(samlSettings.getProviderId()).thenReturn(PROVIDER_ID);
    when(samlSettings.getLoginUrl()).thenReturn("invalid");

    assertThatIllegalStateException()
      .isThrownBy(() -> findRegistration(CALLBACK_URL))
      .withMessage("Invalid SAML Login URL");
  }

  private static void assertCommonFields(RelyingPartyRegistration registration, X509Certificate certificate, String callbackUrl) {
    assertThat(registration.getRegistrationId()).isEqualTo("saml");
    assertThat(registration.getAssertionConsumerServiceLocation()).isEqualTo(callbackUrl);
    assertThat(registration.getAssertionConsumerServiceBinding()).isEqualTo(Saml2MessageBinding.POST);
    assertThat(registration.getEntityId()).isEqualTo(APPLICATION_ID);
    assertThat(registration.getAssertingPartyMetadata().getEntityId()).isEqualTo(PROVIDER_ID);
    assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceLocation()).isEqualTo(SSO_URL);
    assertThat(registration.getAssertingPartyMetadata().getVerificationX509Credentials()).containsExactly(Saml2X509Credential.verification(certificate));
  }

  private X509Certificate mockCertificate() {
    X509Certificate mockedCertificate = mock();
    when(samlCertificateConverter.toX509Certificate(CERTIF_STRING)).thenReturn(mockedCertificate);
    return mockedCertificate;
  }

  private X509Certificate mockServiceProviderCertificate() {
    X509Certificate mockedCertificate = mock();
    when(samlCertificateConverter.toX509Certificate(CERTIF_SP_STRING)).thenReturn(mockedCertificate);
    return mockedCertificate;
  }

  private PrivateKey mockPrivateKey() {
    PrivateKey privateKey = mock();
    when(samlPrivateKeyConverter.toPrivateKey(PRIVATE_KEY)).thenReturn(privateKey);
    return privateKey;
  }

  private RelyingPartyRegistration findRegistration(@Nullable String callbackUrl) {
    SonarqubeRelyingPartyRegistrationRepository relyingPartyRegistrationRepository = new SonarqubeRelyingPartyRegistrationRepository(samlSettings, samlCertificateConverter,
      samlPrivateKeyConverter, callbackUrl);
    return relyingPartyRegistrationRepository.findByRegistrationId("registrationId");
  }

}
