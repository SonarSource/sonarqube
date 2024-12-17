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

import java.util.Collection;
import java.util.function.Supplier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;

import static org.springframework.security.saml2.core.Saml2ErrorCodes.INVALID_IN_RESPONSE_TO;
import static org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import static org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.createDefaultResponseValidator;

public class SonarqubeSaml2ResponseValidator implements Converter<ResponseToken, Saml2ResponseValidatorResult> {

  private final Converter<ResponseToken, Saml2ResponseValidatorResult> delegate = createDefaultResponseValidator();

  private Supplier<String> validInResponseToSupplier;

  @Override
  public Saml2ResponseValidatorResult convert(ResponseToken responseToken) {
    Saml2ResponseValidatorResult result = delegate.convert(responseToken);

    String inResponseTo = responseToken.getResponse().getInResponseTo();
    validInResponseToSupplier = () -> inResponseTo;

    Collection<Saml2Error> errors = removeInResponseToErrorIfPresent(result);

    return Saml2ResponseValidatorResult.failure(errors);
  }

  public Supplier<String> getValidInResponseToSupplier() {
    return validInResponseToSupplier;
  }

  private Collection<Saml2Error> removeInResponseToErrorIfPresent(Saml2ResponseValidatorResult result) {
    return result.getErrors().stream()
      .filter(error -> !error.getErrorCode().equals(INVALID_IN_RESPONSE_TO))
      .toList();
  }
}
