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
package org.sonar.core.sarif;

import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import static java.lang.String.format;
import static org.sonar.core.sarif.Sarif210.SARIF_VERSION;

public class SarifVersionValidator {
  public static final Set<String> SUPPORTED_SARIF_VERSIONS = Set.of(SARIF_VERSION);
  public static final String UNSUPPORTED_VERSION_MESSAGE_TEMPLATE = "Version [%s] of SARIF is not supported";

  private SarifVersionValidator() {}

  public static void validateSarifVersion(@Nullable String version) {
    if (!isSupportedSarifVersion(version)) {
      throw new IllegalStateException(composeUnsupportedVersionMessage(version));
    }
  }

  private static boolean isSupportedSarifVersion(@Nullable String version) {
    return Optional.ofNullable(version)
      .filter(SUPPORTED_SARIF_VERSIONS::contains)
      .isPresent();
  }

  private static String composeUnsupportedVersionMessage(@Nullable String version) {
    return format(UNSUPPORTED_VERSION_MESSAGE_TEMPLATE, version);
  }
}
