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
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.client.components.ShowRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ComponentsWsTest {

  private static final String FILE_KEY = "sample:src/main/xoo/sample/Sample.xoo";

  @ClassRule
  public static final Orchestrator orchestrator = ComponentSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void setUp() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  @Test
  public void show() {
    Components.ShowWsResponse response = tester.wsClient().components().show(new ShowRequest().setComponent(FILE_KEY));

    assertThat(response).isNotNull();
    assertThat(response.getComponent().getKey()).isEqualTo(FILE_KEY);
    assertThat(response.getAncestorsList()).isNotEmpty();
  }
}
