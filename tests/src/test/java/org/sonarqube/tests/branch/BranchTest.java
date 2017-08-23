/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonarqube.tests.branch;

import com.sonar.orchestrator.Orchestrator;
import org.assertj.core.groups.Tuple;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category1Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsBranches;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

public class BranchTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void list_branches_contains_main_branch() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    WsBranches.ListWsResponse result = tester.wsClient().projectBranches().list("sample");

    assertThat(result.getBranchesList())
      .extracting(WsBranches.Branch::getName, WsBranches.Branch::getType, WsBranches.Branch::getIsMain)
      .containsExactlyInAnyOrder(Tuple.tuple("master", Common.BranchType.LONG, true));
  }
}
