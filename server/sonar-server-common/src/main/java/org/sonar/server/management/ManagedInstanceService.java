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
package org.sonar.server.management;

import java.util.Map;
import java.util.Set;
import org.sonar.db.DbSession;

public interface ManagedInstanceService {

  int DELEGATING_INSTANCE_PRIORITY = 1;

  boolean isInstanceExternallyManaged();

  String getProviderName();

  Map<String, Boolean> getUserUuidToManaged(DbSession dbSession, Set<String> userUuids);

  Map<String, Boolean> getGroupUuidToManaged(DbSession dbSession, Set<String> groupUuids);

  String getManagedUsersSqlFilter(boolean filterByManaged);

  String getManagedGroupsSqlFilter(boolean filterByManaged);

  boolean isUserManaged(DbSession dbSession, String userUuid);

  boolean isGroupManaged(DbSession dbSession, String groupUuid);

  void queueSynchronisationTask();

}
