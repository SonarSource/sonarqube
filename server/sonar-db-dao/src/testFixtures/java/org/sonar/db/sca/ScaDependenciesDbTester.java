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

import java.util.List;
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

  public ScaDependencyDto newScaDependencyDto(String componentUuid, String scaReleaseUuid, String suffix, boolean direct) {
    long now = System.currentTimeMillis();
      return new ScaDependencyDto("scaDependencyUuid" + suffix,
        scaReleaseUuid,
        direct,
        "compile",
        "pom.xml",
        "package-lock.json",
        List.of(List.of("pkg:npm/foo@1.0.0")),
        now,
        now
      );
  }

  public ScaDependencyDto insertScaDependency(String componentUuid, String scaReleaseUuid, String suffix, boolean direct) {
    ScaDependencyDto scaDependencyDto = newScaDependencyDto(componentUuid, scaReleaseUuid, suffix, direct);
    dbClient.scaDependenciesDao().insert(db.getSession(), scaDependencyDto);
    return scaDependencyDto;
  }

  public ScaDependencyDto insertScaDependency(String componentUuid, ScaReleaseDto scaReleaseDto, String suffix, boolean direct) {
    return insertScaDependency(componentUuid, scaReleaseDto.uuid(), suffix, direct);
  }

  public ScaDependencyDto insertScaDependencyWithRelease(String componentUuid, String suffix, boolean direct, PackageManager packageManager, String packageName) {
    var scaReleaseDto = db.getScaReleasesDbTester().insertScaRelease(componentUuid, suffix, packageManager, packageName);
    return insertScaDependency(componentUuid, scaReleaseDto.uuid(), suffix, direct);
  }
}
