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
package org.sonar.scanner.ci;

import java.util.Optional;
import javax.annotation.Nullable;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;

public class CiConfigurationImpl implements CiConfiguration {

  @Nullable
  private final String scmRevision;

  public CiConfigurationImpl(@Nullable String scmRevision) {
    this.scmRevision = defaultIfBlank(scmRevision, null);
  }

  @Override
  public Optional<String> getScmRevision() {
    return Optional.ofNullable(scmRevision);
  }
}
