/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.scan;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.notifications.AnalysisWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.scanner.bootstrap.ScannerWsClientProvider;

public class DeprecatedPropertiesWarningGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedPropertiesWarningGenerator.class);

  @VisibleForTesting
  static final String PASSWORD_WARN_MESSAGE = String.format("The properties '%s' and '%s' are deprecated and will be removed in the " +
      "future. Please pass a token with the '%s' property instead.", CoreProperties.LOGIN, CoreProperties.PASSWORD,
    ScannerWsClientProvider.TOKEN_PROPERTY);
  @VisibleForTesting
  static final String LOGIN_WARN_MESSAGE = String.format("The property '%s' is deprecated and will be removed in the future. " +
    "Please use the '%s' property instead when passing a token.", CoreProperties.LOGIN, ScannerWsClientProvider.TOKEN_PROPERTY);
  @VisibleForTesting
  static final String SCANNER_DOTNET_WARN_MESSAGE = String.format(" The '%s' property is available from SonarScanner " +
    "for .NET version 5.13.", ScannerWsClientProvider.TOKEN_PROPERTY);
  private static final String ENV_KEY_SCANNER_DOTNET = "ScannerMSBuild";

  private final Configuration configuration;
  private final AnalysisWarnings analysisWarnings;
  private final EnvironmentInformation environmentInformation;

  public DeprecatedPropertiesWarningGenerator(Configuration configuration, AnalysisWarnings analysisWarnings,
                                              EnvironmentInformation environmentInformation) {
    this.configuration = configuration;
    this.analysisWarnings = analysisWarnings;
    this.environmentInformation = environmentInformation;
  }

  public void execute() {
    Optional<String> login = configuration.get(CoreProperties.LOGIN);
    Optional<String> password = configuration.get(CoreProperties.PASSWORD);

    String warningMessage = null;
    if (password.isPresent()) {
      warningMessage = PASSWORD_WARN_MESSAGE;
    } else if (login.isPresent()) {
      warningMessage = LOGIN_WARN_MESSAGE;
    }

    if (warningMessage != null) {
      if (isScannerDotNet()) {
        warningMessage += SCANNER_DOTNET_WARN_MESSAGE;
      }
      LOG.warn(warningMessage);
      analysisWarnings.addUnique(warningMessage);
    }
  }

  private boolean isScannerDotNet() {
    return StringUtils.containsIgnoreCase(environmentInformation.getKey(), ENV_KEY_SCANNER_DOTNET);
  }
}
