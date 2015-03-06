/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.server.component.ComponentService;
import org.sonar.server.db.DbClient;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TestsWsTest {

  WebService.Controller controller;

  @Before
  public void setUp() throws Exception {
    SnapshotPerspectives snapshotPerspectives = mock(SnapshotPerspectives.class);
    WsTester tester = new WsTester(new TestsWs(
      new TestsShowAction(mock(DbClient.class), snapshotPerspectives),
      new TestsTestCasesAction(snapshotPerspectives, mock(ComponentService.class), mock(DbClient.class)),
      new TestsCoveredFilesAction(snapshotPerspectives)));
    controller = tester.controller("api/tests");
  }

  @Test
  public void define_controller() throws Exception {
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("4.4");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(3);
  }

  @Test
  public void define_show_action() throws Exception {
    WebService.Action action = controller.action("show");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void define_covered_files_action() throws Exception {
    WebService.Action action = controller.action("covered_files");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_test_cases_action() throws Exception {
    WebService.Action action = controller.action("test_cases");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(3);
  }

}
