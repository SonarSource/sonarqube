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
package org.sonar.server.user.ws;

import java.util.List;

public interface HomepageTypes {

  enum Type {
    PROJECT,
    /**
     * These types are only available on SonarQube
     */
    PROJECTS, ISSUES, PORTFOLIOS, PORTFOLIO, APPLICATION,
    /**
     * These types are only available on SonarCloud
     */
    MY_PROJECTS, MY_ISSUES,
    /**
     * This type is only available when organizations are enabled
     */
    ORGANIZATION
  }

  List<Type> getTypes();

  Type getDefaultType();
}
