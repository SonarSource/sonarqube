/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.KeyWithUuidDto;

public class ComponentUuidFactoryImpl implements ComponentUuidFactory {
  private final Map<String, String> uuidsByKey = new HashMap<>();

  /** For the sake of consistency it is important that sub-portfolios have the same uuid as the associated component.
   * As sub-portfolio have no component associated with their creation, it is necessary to look search for their uuid.
   * see SONAR-19407 for more info
   *
   * @param dbClient
   * @param dbSession
   * @param rootKey
   */
  public ComponentUuidFactoryImpl(DbClient dbClient, DbSession dbSession, String rootKey) {
    List<KeyWithUuidDto> projectKeys = dbClient.componentDao().selectUuidsByKeyFromProjectKey(dbSession, rootKey);
    List<KeyWithUuidDto> portFolioKeys = dbClient.portfolioDao().selectUuidsByKey(dbSession, rootKey);
    projectKeys.forEach(dto -> uuidsByKey.put(dto.key(), dto.uuid()));
    portFolioKeys.forEach(dto -> uuidsByKey.putIfAbsent(dto.key(), dto.uuid()));
  }

  public ComponentUuidFactoryImpl(DbClient dbClient, DbSession dbSession, String rootKey, Branch branch) {
    List<KeyWithUuidDto> keys;
    if (branch.isMain()) {
      keys = dbClient.componentDao().selectUuidsByKeyFromProjectKey(dbSession, rootKey);
    } else if (branch.getType() == BranchType.PULL_REQUEST) {
      keys = dbClient.componentDao().selectUuidsByKeyFromProjectKeyAndPullRequest(dbSession, rootKey, branch.getPullRequestKey());
    } else {
      keys = dbClient.componentDao().selectUuidsByKeyFromProjectKeyAndBranch(dbSession, rootKey, branch.getName());
    }
    keys.forEach(dto -> uuidsByKey.put(dto.key(), dto.uuid()));
  }

  /**
   * Get UUID from database if it exists, otherwise generate a new one.
   */
  @Override
  public String getOrCreateForKey(String key) {
    return uuidsByKey.computeIfAbsent(key, k -> Uuids.create());
  }
}
