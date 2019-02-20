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
package org.sonar.server.permission.index;

import java.util.function.Predicate;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.IndexType.IndexRelationType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

@Immutable
public final class AuthorizationScope {
  private final IndexMainType indexType;
  private final Predicate<IndexPermissions> projectPredicate;

  public AuthorizationScope(IndexRelationType functionalType, Predicate<IndexPermissions> projectPredicate) {
    this.indexType = getAuthorizationIndexType(functionalType);
    this.projectPredicate = requireNonNull(projectPredicate);
  }

  /**
   * @return the identifier of the ElasticSearch type (including it's index name), that corresponds to a certain document type
   */
  private static IndexMainType getAuthorizationIndexType(IndexRelationType functionalType) {
    requireNonNull(functionalType);
    IndexMainType mainType = functionalType.getMainType();
    checkArgument(
      TYPE_AUTHORIZATION.equals(mainType.getType()),
      "Index %s doesn't seem to be an authorized index as main type is not %s (got %s)",
      mainType.getIndex(), TYPE_AUTHORIZATION, mainType.getType());
    return mainType;
  }

  /**
   * Identifier of the authorization type (in the same index than the original IndexType, passed into the constructor).
   */
  public IndexMainType getIndexType() {
    return indexType;
  }

  /**
   * Predicates that filters the projects to be involved in authorization.
   */
  public Predicate<IndexPermissions> getProjectPredicate() {
    return projectPredicate;
  }
}
