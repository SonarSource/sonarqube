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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SonarqubeSaml2ResponseValidatorTest {

  @Mock
  private Converter<ResponseToken, Saml2ResponseValidatorResult> delegate;

  @Mock
  private SamlMessageIdChecker samlMessageIdChecker;

  @InjectMocks
  private SonarqubeSaml2ResponseValidator sonarqubeSaml2ResponseValidator;

  @Test
  void convert_ifResponseIdAlreadyUsed_shouldReturnFailure() {
    ResponseToken responseToken = mockResponseToken();

    when(delegate.convert(responseToken)).thenReturn(Saml2ResponseValidatorResult.success());
    Saml2Error saml2Error = mock();
    when(samlMessageIdChecker.validateMessageIdWasNotAlreadyUsed("responseId")).thenReturn(Optional.of(saml2Error));

    Saml2ResponseValidatorResult validatorResult = sonarqubeSaml2ResponseValidator.convert(responseToken);

    assertThat(validatorResult.getErrors()).containsExactly(saml2Error);
  }

  @Test
  void convert_returnsErrorFromDelegate() {
    ResponseToken responseToken = mockResponseToken();
    Saml2Error saml2Error = mock(RETURNS_DEEP_STUBS);

    when(delegate.convert(responseToken)).thenReturn(Saml2ResponseValidatorResult.failure(saml2Error, saml2Error));

    Saml2ResponseValidatorResult validatorResult = sonarqubeSaml2ResponseValidator.convert(responseToken);

    assertThat(validatorResult.getErrors()).containsExactly(saml2Error, saml2Error);
  }

  @Test
  void convert_filtersOutInResponseToValidationErrors() {
    ResponseToken responseToken = mockResponseToken();
    Saml2Error inResponseToError = new Saml2Error(Saml2ErrorCodes.INVALID_IN_RESPONSE_TO, "description");
    Saml2Error otherError = new Saml2Error("other", "description");

    when(delegate.convert(responseToken)).thenReturn(Saml2ResponseValidatorResult.failure(inResponseToError, otherError));

    Saml2ResponseValidatorResult validatorResult = sonarqubeSaml2ResponseValidator.convert(responseToken);

    assertThat(validatorResult.getErrors()).containsExactly(otherError);
  }

  private static ResponseToken mockResponseToken() {
    ResponseToken responseToken = mock(RETURNS_DEEP_STUBS);
    when(responseToken.getResponse().getID()).thenReturn("responseId");
    return responseToken;
  }
}
