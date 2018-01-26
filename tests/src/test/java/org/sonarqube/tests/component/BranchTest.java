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
package org.sonarqube.tests.component;

import com.sonar.orchestrator.Orchestrator;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.ProjectBranches;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.projectbranches.ListRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

public class BranchTest {

  @ClassRule
  public static Orchestrator orchestrator = ComponentSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void list_branches_contains_main_branch() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    ProjectBranches.ListWsResponse result = tester.wsClient().projectBranches().list(new ListRequest().setProject("sample"));

    assertThat(result.getBranchesList())
      .extracting(ProjectBranches.Branch::getName, ProjectBranches.Branch::getType, ProjectBranches.Branch::getIsMain)
      .containsExactlyInAnyOrder(Tuple.tuple("master", Common.BranchType.LONG, true));
  }

  @Test
  public void navigation_global_return_branches_support_to_false() {
    WsResponse status = tester.wsClient().wsConnector().call(new GetRequest("api/navigation/global"));
    Map<String, Object> statusMap = ItUtils.jsonToMap(status.content());

    assertThat(statusMap.get("branchesEnabled")).isEqualTo(false);
  }
}
