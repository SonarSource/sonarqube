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

public class ScaReleasesDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ScaReleasesDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public static ScaReleaseDto newScaReleaseDto(String componentUuid, String suffix, PackageManager packageManager, String packageName) {
    return new ScaReleaseDto("scaReleaseUuid" + suffix,
      componentUuid,
      "packageUrl" + suffix,
      packageManager,
      packageName,
      "1.0.0-" + suffix,
      "MIT",
      true,
      false,
      1L,
      2L);
  }

  public ComponentDto newComponentDto(String branchUuid, String suffix) {
    return new ComponentDto().setUuid("uuid" + suffix)
      .setKey("key" + suffix)
      .setUuidPath("uuidPath" + suffix)
      .setBranchUuid(branchUuid);
  }

  public ComponentDto insertComponent(String branchUuid, String suffix) {
    ComponentDto componentDto = newComponentDto(branchUuid, suffix);
    db.components().insertComponent(componentDto);
    return componentDto;
  }

  public ScaReleaseDto insertScaRelease(String componentUuid, String suffix) {
    return insertScaRelease(componentUuid, suffix, PackageManager.MAVEN, "packageName" + suffix);
  }

  public ScaReleaseDto insertScaRelease(String componentUuid, String suffix, PackageManager packageManager, String packageName) {
    var scaReleaseDto = newScaReleaseDto(componentUuid, suffix, packageManager, packageName);
    dbClient.scaReleasesDao().insert(db.getSession(), scaReleaseDto);
    return scaReleaseDto;
  }

  /**
   * Inserts a release and also dependencyCount sca_dependencies that depend on that release.
   */
  public ScaReleaseDto insertScaReleaseWithDependency(String componentUuid, String suffix, int dependencyCount, boolean direct, PackageManager packageManager, String packageName) {
    var scaReleaseDto = insertScaRelease(componentUuid, suffix, packageManager, packageName);
    while (dependencyCount > 0) {
      db.getScaDependenciesDbTester().insertScaDependency(componentUuid, scaReleaseDto, suffix + "-" + dependencyCount, direct);
      dependencyCount--;
    }
    return scaReleaseDto;
  }
}
