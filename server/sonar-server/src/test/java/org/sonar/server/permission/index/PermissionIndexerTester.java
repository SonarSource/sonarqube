/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Arrays;
import java.util.stream.Stream;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.EsTester;

import static java.util.Arrays.asList;

public class PermissionIndexerTester {

  private final PermissionIndexer permissionIndexer;

  public PermissionIndexerTester(EsTester esTester, NeedAuthorizationIndexer indexer, NeedAuthorizationIndexer... others) {
    NeedAuthorizationIndexer[] indexers = Stream.concat(Stream.of(indexer), Arrays.stream(others)).toArray(i -> new NeedAuthorizationIndexer[i]);
    this.permissionIndexer = new PermissionIndexer(null, esTester.client(), indexers);
  }

  public PermissionIndexerTester allowOnlyAnyone(ComponentDto project) {
    PermissionIndexerDao.Dto dto = new PermissionIndexerDao.Dto(project.uuid(), System.currentTimeMillis(), project.qualifier())
      .addGroupName("Anyone");
    permissionIndexer.index(asList(dto));
    return this;
  }

  public PermissionIndexerTester allowOnlyUser(ComponentDto project, long userId) {
    PermissionIndexerDao.Dto dto = new PermissionIndexerDao.Dto(project.uuid(), System.currentTimeMillis(), project.qualifier())
      .addUser(userId);
    permissionIndexer.index(asList(dto));
    return this;
  }

  public PermissionIndexerTester allowOnlyGroup(ComponentDto project, String groupName) {
    PermissionIndexerDao.Dto dto = new PermissionIndexerDao.Dto(project.uuid(), System.currentTimeMillis(), project.qualifier())
      .addGroupName(groupName);
    permissionIndexer.index(asList(dto));
    return this;
  }

  public PermissionIndexerTester allow(PermissionIndexerDao.Dto access) {
    permissionIndexer.index(asList(access));
    return this;
  }
}
