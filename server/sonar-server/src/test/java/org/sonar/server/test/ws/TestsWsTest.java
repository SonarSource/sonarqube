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
package org.sonar.server.test.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TestsWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WebService.Controller controller;

  @Before
  public void setUp() {
    WsTester tester = new WsTester(new TestsWs(
      new ListAction(mock(DbClient.class), mock(TestIndex.class), userSessionRule, mock(ComponentFinder.class)),
      new CoveredFilesAction(mock(DbClient.class), mock(TestIndex.class), userSessionRule)));
    controller = tester.controller("api/tests");
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("4.4");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(2);
  }

  @Test
  public void define_list_action() {
    WebService.Action action = controller.action("list");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(8);
    assertThat(action.description()).isEqualTo("Get the list of tests either in a test file or that test a given line of source code.<br /> " +
      "Require Browse permission on the file's project.<br /> " +
      "One (and only one) of the following combination of parameters must be provided: " +
      "<ul>" +
      "<li>testId - get a specific test</li>" +
      "<li>testFileId - get the tests in a test file</li>" +
      "<li>testFileKey - get the tests in a test file</li>" +
      "<li>sourceFileId and sourceFileLineNumber - get the tests that cover a specific line of code</li>" +
      "<li>sourceFileKey and sourceFileLineNumber - get the tests that cover a specific line of code</li>" +
      "</ul>");
  }

  @Test
  public void define_covered_files() {
    WebService.Action action = controller.action("covered_files");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(3);
  }
}
