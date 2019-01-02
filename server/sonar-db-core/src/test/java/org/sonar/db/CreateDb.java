/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import org.apache.commons.lang.StringUtils;

import java.io.File;

import static com.sonar.orchestrator.container.Edition.COMMUNITY;

public class CreateDb {

  public static void main(String[] args) {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    builder.setEdition(COMMUNITY);
    String version = System.getProperty("sonar.runtimeVersion");
    if (StringUtils.isEmpty(version)) {
      Location zip = FileLocation.byWildcardMavenFilename(new File("../../sonar-application/build/distributions"), "sonar-application-*.zip");
      builder.setZipLocation(zip);
    } else {
      builder.setSonarVersion(version);
    }
    builder.setOrchestratorProperty("orchestrator.workspaceDir", "build/it");

    Orchestrator orchestrator = builder.build();
    try {
      orchestrator.start();
    } finally {
      orchestrator.stop();
    }
  }
}
