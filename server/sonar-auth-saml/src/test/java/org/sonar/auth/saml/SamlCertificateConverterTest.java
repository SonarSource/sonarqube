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

import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

class SamlCertificateConverterTest {
  private static final String VALID_CERTIFICATE_STRING = """
    -----BEGIN CERTIFICATE-----
    MIIDqDCCApCgAwIBAgIGAYcJtZATMA0GCSqGSIb3DQEBCwUAMIGUMQswCQYDVQQGEwJVUzETMBEG
    A1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsGA1UECgwET2t0YTEU
    MBIGA1UECwwLU1NPUHJvdmlkZXIxFTATBgNVBAMMDGRldi05ODIxMjUzNzEcMBoGCSqGSIb3DQEJ
    ARYNaW5mb0Bva3RhLmNvbTAeFw0yMzAzMjIxNDI0MDZaFw0zMzAzMjIxNDI1MDZaMIGUMQswCQYD
    VQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsG
    A1UECgwET2t0YTEUMBIGA1UECwwLU1NPUHJvdmlkZXIxFTATBgNVBAMMDGRldi05ODIxMjUzNzEc
    MBoGCSqGSIb3DQEJARYNaW5mb0Bva3RhLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC
    ggEBALNloIuL4r7mUDkZcD7hdYrUw335hlRGWFjMV1OjRFhI/hMw2NUq+KgQjyte6zpE7a9nMlWw
    lRqkEVAuCW7ZcjO/Wk1TECiKwS2nYN3InPuuF6TCk0/gJSFZuiKXdtUUDod5viNJyEXb0Ol8rtIl
    TRffbSRiaWPvPykhtDZVObS0QDpBo4wVK1C+G+3e0/P/YCD6g4+zJWFYT4sbY6Ee97xhVwcdO6ZS
    jfba6lYtmUCUwRPRLQPkM9xAjKinVu5mmNPY8sXuxIRs/yEvhxnhTOnbvnU5oNU5DWI28vAiMOlD
    SpQTUQZjqLDa9AHyvkWT/j0WU5AI1IFgLqB5gg6dY8UCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEA
    DRAEvjil9vPgCMJSYl3x5i83is4JlZ6SeN8mXxJfj35pQb+sLa+XrnITnAk6fnYX4NYYEwDGD+Vq
    AnSIRsJEeMYTnQWMGLp5er88IDltDlfIMSs8WgVxWkJ6R66BVGFQRVo9IJQRVuBXrPTahL43ZBn1
    SynJxMl9tceAb6Q18ncyK9DpsLfrgpkerPcLjhjWiCl9iEpfUEzGEeLzin9OyfSwTtMWcPLrqgUb
    nWiSEIvNnGzGVQunZaUF4cLxlstgWJzsWLcuzr0cdSO7eIsAtAMVDqXY1ESpewRYqzDeXmj+eKso
    k5X4rDjQIGfE0XskScXfKyY7CVklfmW1dCuzdw==
    -----END CERTIFICATE-----
    """;

  private static final String VALID_CERTIFICATE_STRING_WITHOUT_COMMENTS = """
    MIIDqDCCApCgAwIBAgIGAYcJtZATMA0GCSqGSIb3DQEBCwUAMIGUMQswCQYDVQQGEwJVUzETMBEG
    A1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsGA1UECgwET2t0YTEU
    MBIGA1UECwwLU1NPUHJvdmlkZXIxFTATBgNVBAMMDGRldi05ODIxMjUzNzEcMBoGCSqGSIb3DQEJ
    ARYNaW5mb0Bva3RhLmNvbTAeFw0yMzAzMjIxNDI0MDZaFw0zMzAzMjIxNDI1MDZaMIGUMQswCQYD
    VQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsG
    A1UECgwET2t0YTEUMBIGA1UECwwLU1NPUHJvdmlkZXIxFTATBgNVBAMMDGRldi05ODIxMjUzNzEc
    MBoGCSqGSIb3DQEJARYNaW5mb0Bva3RhLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC
    ggEBALNloIuL4r7mUDkZcD7hdYrUw335hlRGWFjMV1OjRFhI/hMw2NUq+KgQjyte6zpE7a9nMlWw
    lRqkEVAuCW7ZcjO/Wk1TECiKwS2nYN3InPuuF6TCk0/gJSFZuiKXdtUUDod5viNJyEXb0Ol8rtIl
    TRffbSRiaWPvPykhtDZVObS0QDpBo4wVK1C+G+3e0/P/YCD6g4+zJWFYT4sbY6Ee97xhVwcdO6ZS
    jfba6lYtmUCUwRPRLQPkM9xAjKinVu5mmNPY8sXuxIRs/yEvhxnhTOnbvnU5oNU5DWI28vAiMOlD
    SpQTUQZjqLDa9AHyvkWT/j0WU5AI1IFgLqB5gg6dY8UCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEA
    DRAEvjil9vPgCMJSYl3x5i83is4JlZ6SeN8mXxJfj35pQb+sLa+XrnITnAk6fnYX4NYYEwDGD+Vq
    AnSIRsJEeMYTnQWMGLp5er88IDltDlfIMSs8WgVxWkJ6R66BVGFQRVo9IJQRVuBXrPTahL43ZBn1
    SynJxMl9tceAb6Q18ncyK9DpsLfrgpkerPcLjhjWiCl9iEpfUEzGEeLzin9OyfSwTtMWcPLrqgUb
    nWiSEIvNnGzGVQunZaUF4cLxlstgWJzsWLcuzr0cdSO7eIsAtAMVDqXY1ESpewRYqzDeXmj+eKso
    k5X4rDjQIGfE0XskScXfKyY7CVklfmW1dCuzdw==
    """;

  private final SamlCertificateConverter samlCertificateConverter = new SamlCertificateConverter();

  @ParameterizedTest
  @ValueSource(strings = {VALID_CERTIFICATE_STRING, VALID_CERTIFICATE_STRING_WITHOUT_COMMENTS})
  void toX509Certificate_whenStringIsValid_succeeds(String certificate) {
    X509Certificate x509Certificate = samlCertificateConverter.toX509Certificate(certificate);
    assertThat(x509Certificate).isNotNull();
  }

  @Test
  void toX509Certificate_whenStringIsInvalid_throwsException() {
    String invalidCertificateString = "invalid";
    assertThatRuntimeException().isThrownBy(() -> samlCertificateConverter.toX509Certificate(invalidCertificateString));
  }

}
