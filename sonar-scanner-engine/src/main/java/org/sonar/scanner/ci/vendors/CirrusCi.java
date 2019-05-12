/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Support https://cirrus-ci.org/
 *
 * Environment variables are documented at https://cirrus-ci.org/guide/writing-tasks/#environment-variables
 */
public class CirrusCi implements CiVendor {

  private static final String PROPERTY_COMMIT = "CIRRUS_CHANGE_IN_REPO";
  private final System2 system;

  public CirrusCi(System2 system) {
    this.system = system;
  }

  @Override
  public String getName() {
    return "CirrusCI";
  }

  @Override
  public boolean isDetected() {
    return "true".equals(system.envVariable("CIRRUS_CI"));
  }

  @Override
  public CiConfiguration loadConfiguration() {
    String revision = system.envVariable(PROPERTY_COMMIT);
    if (isEmpty(revision)) {
      Loggers.get(getClass()).warn("Missing environment variable " + PROPERTY_COMMIT);
    }
    return new CiConfigurationImpl(revision);
  }
}
