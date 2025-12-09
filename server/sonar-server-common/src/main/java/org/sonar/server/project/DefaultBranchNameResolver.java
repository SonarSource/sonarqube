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
package org.sonar.server.project;

import org.sonar.api.config.Configuration;

import static org.sonar.core.config.CorePropertyDefinitions.SONAR_PROJECTCREATION_MAINBRANCHNAME;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class DefaultBranchNameResolver {

  Configuration configuration;

  public DefaultBranchNameResolver(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getEffectiveMainBranchName() {
    return configuration.get(SONAR_PROJECTCREATION_MAINBRANCHNAME).orElse(DEFAULT_MAIN_BRANCH_NAME);
  }
}
