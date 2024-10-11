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
package org.sonar.db.dependency;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class ProjectDependenciesDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ProjectDependenciesDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public ProjectDependencyDto insertProjectDependency(String branchUuid) {
    return insertProjectDependency(branchUuid, EMPTY, null);
  }

  public ProjectDependencyDto insertProjectDependency(String branchUuid, String suffix) {
    return insertProjectDependency(branchUuid, suffix, null);
  }

  public ProjectDependencyDto insertProjectDependency(String branchUuid, String suffix, @Nullable Consumer<ComponentDto> dtoPopulator) {
    ComponentDto componentDto = new ComponentDto().setUuid("uuid" + suffix)
      .setKey("key" + suffix)
      .setUuidPath("uuidPath" + suffix)
      .setName("name" + suffix)
      .setLongName("long_name" + suffix)
      .setBranchUuid(branchUuid);

    if (dtoPopulator != null) {
      dtoPopulator.accept(componentDto);
    }

    db.components().insertComponent(componentDto);
    var projectDependencyDto = new ProjectDependencyDto(componentDto.uuid(), "version" + suffix, "includePaths" + suffix, "packageManager" + suffix, 1L, 2L);
    dbClient.projectDependenciesDao().insert(db.getSession(), projectDependencyDto);
    return projectDependencyDto;
  }
}
