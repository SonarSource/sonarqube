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
package org.sonar.scanner.ci.vendors;

import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;

public class Bitrise implements CiVendor {

  private final System2 system2;

  public Bitrise(System2 system2) {
    this.system2 = system2;
  }

  @Override
  public String getName() {
    return "Bitrise";
  }

  @Override
  public boolean isDetected() {
    return environmentVariableIsTrue("CI") && environmentVariableIsTrue("BITRISE_IO");
  }

  @Override
  public CiConfiguration loadConfiguration() {
    String revision = system2.envVariable("BITRISE_GIT_COMMIT");
    return new CiConfigurationImpl(revision, getName());
  }

  private boolean environmentVariableIsTrue(String key) {
    return Optional.ofNullable(system2.envVariable(key))
      .map(Boolean::parseBoolean)
      .orElse(false);
  }
}
