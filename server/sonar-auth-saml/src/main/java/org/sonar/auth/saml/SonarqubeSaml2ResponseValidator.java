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

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;

import static org.springframework.security.saml2.core.Saml2ErrorCodes.INVALID_IN_RESPONSE_TO;
import static org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken;

class SonarqubeSaml2ResponseValidator implements Converter<ResponseToken, Saml2ResponseValidatorResult> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SonarqubeSaml2ResponseValidator.class);

  private final Converter<ResponseToken, Saml2ResponseValidatorResult> delegate;
  private final SamlMessageIdChecker samlMessageIdChecker;

  @Inject
  SonarqubeSaml2ResponseValidator(SamlMessageIdChecker samlMessageIdChecker) {
    this(samlMessageIdChecker, OpenSaml5AuthenticationProvider.ResponseValidator.withDefaults());
  }

  @VisibleForTesting
  SonarqubeSaml2ResponseValidator(SamlMessageIdChecker samlMessageIdChecker, Converter<ResponseToken, Saml2ResponseValidatorResult> delegate) {
    this.samlMessageIdChecker = samlMessageIdChecker;
    this.delegate = delegate;
  }

  @Override
  public Saml2ResponseValidatorResult convert(ResponseToken responseToken) {
    Saml2ResponseValidatorResult validationResults = delegate.convert(responseToken);

    List<Saml2Error> errors = new ArrayList<>(getValidationErrorsWithoutInResponseTo(validationResults));
    samlMessageIdChecker.validateMessageIdWasNotAlreadyUsed(responseToken.getResponse().getID()).ifPresent(errors::add);

    LOGGER.debug("Saml validation errors: {}", errors);
    return Saml2ResponseValidatorResult.failure(errors);
  }

  private static Collection<Saml2Error> getValidationErrorsWithoutInResponseTo(@Nullable Saml2ResponseValidatorResult validationResults) {
    if (validationResults == null) {
      return List.of();
    }

    return removeInResponseToError(validationResults);
  }

  private static Collection<Saml2Error> removeInResponseToError(Saml2ResponseValidatorResult result) {
    return result.getErrors().stream()
      .filter(error -> !INVALID_IN_RESPONSE_TO.equals(error.getErrorCode()))
      .toList();
  }
}
