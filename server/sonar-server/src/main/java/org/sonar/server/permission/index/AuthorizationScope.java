/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.permission.index;

import java.util.function.Predicate;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public final class AuthorizationScope {
  private final String indexName;
  private final Predicate<PermissionIndexerDao.Dto> projectPredicate;

  public AuthorizationScope(String indexName, Predicate<PermissionIndexerDao.Dto> projectPredicate) {
    this.indexName = requireNonNull(indexName);
    this.projectPredicate = requireNonNull(projectPredicate);
  }

  /**
   * Name of the index containing a type named {@link AuthorizationTypeSupport#TYPE_AUTHORIZATION}.
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * Predicates that filters the projects to be involved in
   * authorization.
   */
  public Predicate<PermissionIndexerDao.Dto> getProjectPredicate() {
    return projectPredicate;
  }
}
