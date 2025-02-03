/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class ScaDependenciesDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ScaDependenciesDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public ComponentDto newPackageComponentDto(String branchUuid, String suffix, @Nullable Consumer<ComponentDto> dtoPopulator) {
    var name = "foo:bar";
    ComponentDto componentDto = new ComponentDto().setUuid("uuid" + suffix)
      .setKey("key" + suffix)
      .setUuidPath("uuidPath" + suffix)
      .setName(name + suffix)
      .setLongName("long_name" + suffix)
      .setBranchUuid(branchUuid);

    if (dtoPopulator != null) {
      dtoPopulator.accept(componentDto);
    }

    return componentDto;
  }

  public ScaDependencyDto newScaDependencyDto(ComponentDto componentDto, String suffix) {
    return new ScaDependencyDto("scaDependencyUuid" + suffix,
      componentDto.uuid(),
      "packageUrl" + suffix,
      PackageManager.MAVEN,
      componentDto.name(),
      "1.0.0",
      true,
      "compile",
      "pom.xml",
      "BSD-3-Clause",
      true,
      1L,
      2L);
  }

  public ScaDependencyDto insertScaDependency(String branchUuid) {
    return insertScaDependency(branchUuid, EMPTY, null);
  }

  public ScaDependencyDto insertScaDependency(String branchUuid, String suffix) {
    return insertScaDependency(branchUuid, suffix, null);
  }

  public ScaDependencyDto insertScaDependency(String branchUuid, String suffix, @Nullable Consumer<ComponentDto> dtoPopulator) {
    var componentDto = newPackageComponentDto(branchUuid, suffix, dtoPopulator);

    db.components().insertComponent(componentDto);
    var scaDependencyDto = newScaDependencyDto(componentDto, suffix);
    dbClient.scaDependenciesDao().insert(db.getSession(), scaDependencyDto);
    return scaDependencyDto;
  }
}
