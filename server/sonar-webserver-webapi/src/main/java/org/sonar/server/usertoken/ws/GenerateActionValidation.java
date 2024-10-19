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
package org.sonar.server.usertoken.ws;

import org.jetbrains.annotations.Nullable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.config.MaxTokenLifetimeOption;
import org.sonar.db.DbSession;
import org.sonar.db.user.TokenType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.SonarEdition.COMMUNITY;
import static org.sonar.api.SonarEdition.DEVELOPER;
import static org.sonar.core.config.CorePropertyDefinitions.CODESCAN_WHITE_LABEL_PRODUCT;
import static org.sonar.core.config.MaxTokenLifetimeOption.NO_EXPIRATION;
import static org.sonar.core.config.TokenExpirationConstants.MAX_ALLOWED_TOKEN_LIFETIME;
import static org.sonar.db.user.TokenType.GLOBAL_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_EXPIRATION_DATE;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_PROJECT_KEY;

public final class GenerateActionValidation {

  private final Configuration configuration;
  private final SonarRuntime sonarRuntime;
  private static final String KEY_AMAZON_WHITE_LABEL_PRODUCT = "AMAZON";

  public GenerateActionValidation(Configuration configuration, SonarRuntime sonarRuntime) {
    this.configuration = configuration;
    this.sonarRuntime = sonarRuntime;
  }

  /**
   * <p>Returns the max allowed token lifetime property based on the Sonar Edition.</p>
   * <p>
   *   <ul>
   *     <li>COMMUNITY and DEVELOPER editions don't allow the selection of a max token lifetime, therefore it always defaults to NO_EXPIRATION</li>
   *     <li>ENTERPRISE and DATACENTER editions support the selection of max token lifetime property and the value is searched in the enum</li>
   *   </ul>
   * </p>
   * @return The max allowed token lifetime.
   */
  public MaxTokenLifetimeOption getMaxTokenLifetimeOption() {
    if (List.of(COMMUNITY, DEVELOPER).contains(sonarRuntime.getEdition())) {
      return NO_EXPIRATION;
    }

    String maxTokenLifetimeProp = configuration.get(MAX_ALLOWED_TOKEN_LIFETIME).orElse(NO_EXPIRATION.getName());
    return MaxTokenLifetimeOption.get(maxTokenLifetimeProp);
  }

  /**
   * <p>Validates if the expiration date of the token is between the minimum and maximum allowed values.</p>
   *
   * @param expirationDate The expiration date
   */
  void validateExpirationDate(@Nullable LocalDateTime expirationDate) {
    MaxTokenLifetimeOption maxTokenLifetime = getMaxTokenLifetimeOption();
    if (expirationDate != null) {
      validateMinExpirationDate(expirationDate);
      validateMaxExpirationDate(maxTokenLifetime, expirationDate);
    } else {
      validateMaxExpirationDate(maxTokenLifetime);
    }
  }

  void validateMaxExpirationDate(MaxTokenLifetimeOption maxTokenLifetime, LocalDateTime expirationDate) {
    maxTokenLifetime.getDays()
      .ifPresent(days -> compareExpirationDateToMaxAllowedLifetime(expirationDate, LocalDateTime.now(ZoneOffset.UTC).plusDays(days)));
  }

  void validateMaxExpirationDate(MaxTokenLifetimeOption maxTokenLifetime) {
    maxTokenLifetime.getDays()
      .ifPresent(days -> {
        throw new IllegalArgumentException(
          String.format("Tokens expiring after %s are not allowed. Please use an expiration date.",
            LocalDateTime.now().plusDays(days).format(DateTimeFormatter.ISO_DATE_TIME)));
      });
  }

  void compareExpirationDateToMaxAllowedLifetime(LocalDateTime expirationDate, LocalDateTime maxExpirationDate) {
    if (expirationDate.isAfter(maxExpirationDate)) {
      throw new IllegalArgumentException(
        String.format("Tokens expiring after %s are not allowed. Please use a valid expiration date.",
          maxExpirationDate.format(DateTimeFormatter.ISO_DATE_TIME)));
    }
  }

  void validateMinExpirationDate(LocalDateTime expiredTokenDateTime) {
    int minExpirationTimeInHours = isAmazonWhiteLabelProduct() ? 8 : 24;
    LocalDateTime minExpirationDateTime = LocalDateTime.now(ZoneOffset.UTC)
      .plusHours(minExpirationTimeInHours)
      .minusMinutes(1);

    if(expiredTokenDateTime.isBefore(minExpirationDateTime)) {
      throw new IllegalArgumentException(
        String.format("The minimum value for parameter %s is %s.", PARAM_EXPIRATION_DATE,
            LocalDateTime.now().plusHours(minExpirationTimeInHours).format(DateTimeFormatter.ISO_DATE_TIME)));
    }
  }

  void validateParametersCombination(UserTokenSupport userTokenSupport, DbSession dbSession, Request request, TokenType tokenType) {
    if (PROJECT_ANALYSIS_TOKEN.equals(tokenType)) {
      validateProjectAnalysisParameters(userTokenSupport, dbSession, request);
    } else if (GLOBAL_ANALYSIS_TOKEN.equals(tokenType)) {
      validateGlobalAnalysisParameters(userTokenSupport, request);
    }
  }

  private void validateProjectAnalysisParameters(UserTokenSupport userTokenSupport, DbSession dbSession, Request request) {
    checkArgument(userTokenSupport.sameLoginAsConnectedUser(request), "A Project Analysis Token cannot be generated for another user.");
    checkArgument(request.param(PARAM_PROJECT_KEY) != null, "A projectKey is needed when creating Project Analysis Token");
    userTokenSupport.validateProjectScanPermission(dbSession, request.param(PARAM_PROJECT_KEY));
  }

  private static void validateGlobalAnalysisParameters(UserTokenSupport userTokenSupport, Request request) {
    checkArgument(userTokenSupport.sameLoginAsConnectedUser(request), "A Global Analysis Token cannot be generated for another user.");
    userTokenSupport.validateGlobalScanPermission();
  }

  public boolean isAmazonWhiteLabelProduct() {
    return configuration.get(CODESCAN_WHITE_LABEL_PRODUCT)
      .map(KEY_AMAZON_WHITE_LABEL_PRODUCT::equals)
      .orElse(false);
  }
}
