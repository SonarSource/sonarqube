/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.security;

import java.util.Collection;

/**
 * Note that prefix "do" for names of methods is reserved for future enhancements, thus should not be used in subclasses.
 *
 * @since 2.14
 * @see SecurityRealm
 */
public abstract class ExternalGroupsProvider {

  /**
   * @return list of groups associated with specified user, or null if such user doesn't exist
   * @throws RuntimeException in case of unexpected error such as connection failure
   */
  public abstract Collection<String> doGetGroups(String username);

}
