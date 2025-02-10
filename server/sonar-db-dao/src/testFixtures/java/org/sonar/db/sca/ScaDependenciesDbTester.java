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

import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

public class ScaDependenciesDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ScaDependenciesDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public ComponentDto newPackageComponentDto(String branchUuid, String suffix) {
    return new ComponentDto().setUuid("uuid" + suffix)
      .setKey("key" + suffix)
      .setUuidPath("uuidPath" + suffix)
      .setName("name" + suffix)
      .setLongName("long_name" + suffix)
      .setBranchUuid(branchUuid);
  }

  public ComponentDto insertComponent(String branchUuid, String suffix) {
    ComponentDto componentDto = newPackageComponentDto(branchUuid, suffix);
    db.components().insertComponent(componentDto);
    return componentDto;
  }

  public ScaDependencyDto newScaDependencyDto(String componentUuid, String suffix, boolean direct, PackageManager packageManager, String packageName) {
    long now = System.currentTimeMillis();
    try {
      return new ScaDependencyDto("scaDependencyUuid" + suffix,
        componentUuid,
        "packageUrl" + suffix,
        packageManager,
        packageName,
        "1.0.0",
        direct,
        "compile",
        "pom.xml",
        "BSD-3-Clause",
        true,
        now,
        now
      );
    } finally {
      try {
        Thread.sleep(5);
      } catch (InterruptedException ignored) {
        // ignore
      }
    }
  }

  public ScaDependencyDto insertScaDependency(String componentUuid, String suffix, boolean direct, PackageManager packageManager, String packageName) {
    ScaDependencyDto scaDependencyDto = newScaDependencyDto(componentUuid, suffix, direct, packageManager, packageName);
    dbClient.scaDependenciesDao().insert(db.getSession(), scaDependencyDto);
    return scaDependencyDto;
  }
}
