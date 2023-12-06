/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonarqube.wsgenerator;

import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpResponse;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.junit4.OrchestratorRuleBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;

import static com.sonar.orchestrator.container.Edition.COMMUNITY;

public class ApiDefinitionDownloader {

  public static void main(String[] args) {
    System.out.println(downloadApiDefinition());
  }

  public static String downloadApiDefinition() {
    OrchestratorRuleBuilder builder = OrchestratorRule.builderEnv()
      .defaultForceAuthentication();
    builder.setEdition(COMMUNITY);
    builder.setZipFile(FileLocation.byWildcardMavenFilename(new File("../sonar-application/build/distributions"), "sonar-application-*.zip").getFile())
      .setOrchestratorProperty("orchestrator.workspaceDir", "build");
    OrchestratorRule orchestrator = builder.setServerProperty("sonar.forceAuthentication", "false")
      .build();

    orchestrator.start();
    try {
      HttpCall httpCall = orchestrator.getServer().newHttpCall("api/webservices/list").setParam("include_internals", "true");
      HttpResponse response = httpCall.execute();
      return response.getBodyAsString();
    } finally {
      orchestrator.stop();
    }
  }
}
