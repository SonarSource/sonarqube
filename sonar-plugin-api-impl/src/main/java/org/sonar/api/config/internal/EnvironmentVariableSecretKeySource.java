/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.api.config.internal;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.System2;

/**
 * Reads the secret key from an environment variable. This suits container and cluster deployments, where placing the
 * same file on every node is impractical, and where the orchestrator can already mount a secret as a variable.
 */
public class EnvironmentVariableSecretKeySource implements SecretKeySource {

  public static final String ENVIRONMENT_VARIABLE = "SONAR_SECRET_KEY";

  /**
   * Holds the key being replaced while the secret key is rotated, so values written with it can still be read.
   */
  public static final String PREVIOUS_KEY_ENVIRONMENT_VARIABLE = "SONAR_PREVIOUS_SECRET_KEY";

  private final System2 system2;
  private final String environmentVariable;

  public EnvironmentVariableSecretKeySource(System2 system2) {
    this(system2, ENVIRONMENT_VARIABLE);
  }

  public EnvironmentVariableSecretKeySource(System2 system2, String environmentVariable) {
    this.system2 = system2;
    this.environmentVariable = environmentVariable;
  }

  @Override
  public Optional<String> loadBase64Key() {
    return Optional.ofNullable(system2.envVariable(environmentVariable)).map(StringUtils::trimToNull);
  }

  @Override
  public String describe() {
    return "the " + environmentVariable + " environment variable";
  }
}
