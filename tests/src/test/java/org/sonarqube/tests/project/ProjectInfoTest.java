/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.client.components.ShowRequest;
import util.XooProjectBuilder;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectInfoTest {

  @ClassRule
  public static Orchestrator orchestrator = OrganizationProjectSuite.ORCHESTRATOR;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void project_name_and_description_should_be_truncated_if_too_long() throws IOException {
    Organizations.Organization organization = tester.organizations().generate();
    File projectDir = temp.newFolder();
    new XooProjectBuilder("sample").setFilesPerModule(0).build(projectDir);
    String longName = repeat("x", 1_000);
    String longDescription = repeat("y", 3_000);

    orchestrator.executeBuild(SonarScanner.create(projectDir,
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.projectDescription", longDescription,
      "sonar.projectName", longName));

    Component createdProject = tester.wsClient().components().show(new ShowRequest().setComponent("sample")).getComponent();
    assertThat(createdProject.getName()).isEqualTo(repeat("x", 497) + "...");
    assertThat(createdProject.getDescription()).isEqualTo(repeat("y", 1_997) + "...");
  }
}
