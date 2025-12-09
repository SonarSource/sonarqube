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

import java.util.Map;
import java.util.function.Consumer;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;

@Configuration
public class SamlConfiguration {

  @Bean
  OpenSaml4AuthenticationProvider openSaml4AuthenticationProvider(SonarqubeSaml2ResponseValidator sonarqubeSaml2ResponseValidator){
    OpenSaml4AuthenticationProvider openSaml4AuthenticationProvider = new OpenSaml4AuthenticationProvider();
    openSaml4AuthenticationProvider.setAssertionValidator(createIgnoringResponseToAssertionValidator(sonarqubeSaml2ResponseValidator));
    openSaml4AuthenticationProvider.setResponseValidator(sonarqubeSaml2ResponseValidator);
    return openSaml4AuthenticationProvider;
  }

  private static Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> createIgnoringResponseToAssertionValidator(
    Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2ResponseValidatorResult> customResponseValidator) {
    Consumer<Map<String, Object>> validationContextParameters = validationContextParameterConsumer(((SonarqubeSaml2ResponseValidator) customResponseValidator));
    return OpenSaml4AuthenticationProvider.createDefaultAssertionValidatorWithParameters(validationContextParameters);
  }

  private static Consumer<Map<String, Object>> validationContextParameterConsumer(SonarqubeSaml2ResponseValidator saml2CustomResponseValidator) {
    return params -> {
      String dynamicValidInResponseToValue = saml2CustomResponseValidator.getValidInResponseToSupplier().get();
      params.put(SAML2AssertionValidationParameters.SC_VALID_IN_RESPONSE_TO, dynamicValidInResponseToValue);
    };
  }

}
