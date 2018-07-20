/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.server.es.IndexType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

@Immutable
public final class AuthorizationScope {
  private final IndexType indexType;
  private final Predicate<IndexPermissions> projectPredicate;

  public AuthorizationScope(IndexType indexType, Predicate<IndexPermissions> projectPredicate) {
    this.indexType = getAuthorizationIndexType(indexType);
    this.projectPredicate = requireNonNull(projectPredicate);
  }

  /**
   * @return the identifier of the ElasticSearch type (including it's index name), that corresponds to a certain document type
   */
  private static IndexType getAuthorizationIndexType(IndexType indexType) {
    requireNonNull(indexType);
    requireNonNull(indexType.getIndex());
    checkArgument(!TYPE_AUTHORIZATION.equals(indexType.getType()), "Authorization types do not have authorization on their own.");
    return new IndexType(indexType.getIndex(), TYPE_AUTHORIZATION);
  }

  /**
   * Identifier of the authorization type (in the same index than the original IndexType, passed into the constructor).
   */
  public IndexType getIndexType() {
    return indexType;
  }

  /**
   * Predicates that filters the projects to be involved in authorization.
   */
  public Predicate<IndexPermissions> getProjectPredicate() {
    return projectPredicate;
  }
}
