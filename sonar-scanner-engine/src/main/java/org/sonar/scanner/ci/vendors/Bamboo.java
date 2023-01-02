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

import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;

public class Bamboo implements CiVendor {
  private final System2 system;

  public Bamboo(System2 system) {
    this.system = system;
  }

  @Override
  public String getName() {
    return "Bamboo";
  }


  @Override
  public boolean isDetected() {
    return system.envVariable("bamboo_buildNumber") != null;
  }

  @Override
  public CiConfiguration loadConfiguration() {
    String revision = system.envVariable("bamboo_planRepository_revision");
    return new CiConfigurationImpl(revision, getName());
  }
}
