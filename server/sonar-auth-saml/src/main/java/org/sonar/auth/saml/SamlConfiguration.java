/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;

@Configuration
public class SamlConfiguration {

  @Bean
  OpenSaml5AuthenticationProvider openSaml5AuthenticationProvider(SonarqubeSaml2ResponseValidator sonarqubeSaml2ResponseValidator) {
    OpenSaml5AuthenticationProvider openSaml5AuthenticationProvider = new OpenSaml5AuthenticationProvider();
    openSaml5AuthenticationProvider.setResponseValidator(sonarqubeSaml2ResponseValidator);
    openSaml5AuthenticationProvider.setAssertionValidator(createIgnoringResponseToAssertionValidator());

    return openSaml5AuthenticationProvider;
  }

  private static Converter<OpenSaml5AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> createIgnoringResponseToAssertionValidator() {
    return assertionToken -> {
      Assertion assertion = assertionToken.getAssertion();
      String inResponseTo = extractInResponseTo(assertion);

      return OpenSaml5AuthenticationProvider.AssertionValidator.builder()
        .validationContextParameters(params -> params.put(SAML2AssertionValidationParameters.SC_VALID_IN_RESPONSE_TO, inResponseTo))
        .build()
        .convert(assertionToken);
    };
  }

  @Nullable
  private static String extractInResponseTo(Assertion assertion) {
    return Optional.ofNullable(assertion.getSubject())
      .map(Subject::getSubjectConfirmations)
      .stream()
      .flatMap(Collection::stream)
      .map(SubjectConfirmation::getSubjectConfirmationData)
      .filter(Objects::nonNull)
      .map(SubjectConfirmationData::getInResponseTo)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

}
