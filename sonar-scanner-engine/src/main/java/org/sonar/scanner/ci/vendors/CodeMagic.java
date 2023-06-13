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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class CodeMagic implements CiVendor {

  private static final Logger LOG = LoggerFactory.getLogger(CodeMagic.class);

  private final System2 system;

  public CodeMagic(System2 system) {
    this.system = system;
  }

  @Override
  public String getName() {
    return "CodeMagic";
  }

  @Override
  public boolean isDetected() {
    return !isEmpty(system.envVariable("FCI_BUILD_ID"));
  }

  @Override
  public CiConfiguration loadConfiguration() {
    String revision = system.envVariable("FCI_COMMIT");
    if (isEmpty(revision)) {
      LOG.warn("Missing environment variable FCI_COMMIT");
    }
    return new CiConfigurationImpl(revision, getName());
  }
}
