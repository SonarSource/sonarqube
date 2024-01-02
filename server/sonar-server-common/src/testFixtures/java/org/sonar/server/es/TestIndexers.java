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
package org.sonar.server.es;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import org.sonar.db.DbSession;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIndexers implements Indexers {

  private final ListMultimap<String, EntityEvent> entityCalls = ArrayListMultimap.create();
  private final ListMultimap<String, BranchEvent> branchCalls = ArrayListMultimap.create();

  @Override
  public void commitAndIndexOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, EntityEvent cause) {
    dbSession.commit();
    entityUuids.forEach(entityUuid -> entityCalls.put(entityUuid, cause));
  }

  @Override
  public void commitAndIndexOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, BranchEvent cause) {
    dbSession.commit();
    branchUuids.forEach(branchUuid -> branchCalls.put(branchUuid, cause));
  }

  public boolean hasBeenCalledForEntity(String entityUuid, EntityEvent expectedCause) {
    assertThat(branchCalls.keySet()).isEmpty();
    return entityCalls.get(entityUuid).contains(expectedCause);
  }

  public boolean hasBeenCalledForEntity(String projectUuid) {
    assertThat(branchCalls.keySet()).isEmpty();
    return entityCalls.containsKey(projectUuid);
  }

  public boolean hasBeenCalledForBranch(String branchUuid, BranchEvent expectedCause) {
    assertThat(entityCalls.keySet()).isEmpty();
    return branchCalls.get(branchUuid).contains(expectedCause);
  }

  public boolean hasBeenCalledForBranch(String branchUuid) {
    assertThat(entityCalls.keySet()).isEmpty();
    return branchCalls.containsKey(branchUuid);
  }
}
