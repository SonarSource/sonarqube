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
package org.sonar.server.permission.index;

import java.util.List;
import java.util.stream.Stream;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;

import static java.util.Arrays.stream;

public class PermissionIndexerTester {

  private final PermissionIndexer permissionIndexer;

  public PermissionIndexerTester(EsTester esTester, NeedAuthorizationIndexer indexer, NeedAuthorizationIndexer... others) {
    NeedAuthorizationIndexer[] indexers = Stream.concat(Stream.of(indexer), stream(others)).toArray(NeedAuthorizationIndexer[]::new);
    this.permissionIndexer = new PermissionIndexer(null, esTester.client(), indexers);
  }

  public PermissionIndexerTester allowOnlyAnyone(ComponentDto... projects) {
    return allow(stream(projects).map(project -> new IndexPermissions(project.uuid(), project.qualifier()).allowAnyone()).toList());
  }

  public PermissionIndexerTester allowOnlyUser(ComponentDto project, UserDto user) {
    IndexPermissions dto = new IndexPermissions(project.uuid(), project.qualifier())
      .addUserUuid(user.getUuid());
    return allow(dto);
  }

  public PermissionIndexerTester allowOnlyGroup(ComponentDto project, GroupDto group) {
    IndexPermissions dto = new IndexPermissions(project.uuid(), project.qualifier())
      .addGroupUuid(group.getUuid());
    return allow(dto);
  }

  public PermissionIndexerTester allow(IndexPermissions... indexPermissions) {
    return allow(stream(indexPermissions).toList());
  }

  public PermissionIndexerTester allow(List<IndexPermissions> indexPermissions) {
    permissionIndexer.index(indexPermissions);
    return this;
  }
}
