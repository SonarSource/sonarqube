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
package org.sonar.server.computation.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.DUMB_PROJECT;

public class TreeRootHolderImplTest {

  private static final ReportComponent SOME_REPORT_COMPONENT_TREE = ReportComponent.builder(PROJECT, 1)
    .addChildren(
      ReportComponent.builder(MODULE, 2)
        .addChildren(ReportComponent.builder(DIRECTORY, 3)
          .addChildren(
            ReportComponent.builder(FILE, 4).build()
          )
          .build())
        .build()
    )
    .build();
  private static final ViewsComponent SOME_VIEWS_COMPONENT_TREE = ViewsComponent.builder(VIEW, 1)
    .addChildren(
      ViewsComponent.builder(VIEW, 2)
        .addChildren(ViewsComponent.builder(PROJECT_VIEW, 3).build())
        .build()
    )
    .build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TreeRootHolderImpl underTest = new TreeRootHolderImpl();

  @Test
  public void setRoot_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("root can not be null");

    underTest.setRoot(null);
  }

  @Test
  public void setRoot_throws_ISE_when_called_twice() {
    underTest.setRoot(DUMB_PROJECT);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("root can not be set twice in holder");

    underTest.setRoot(DUMB_PROJECT);
  }

  @Test
  public void getRoot_throws_ISE_if_root_has_not_been_set_yet() {
    expectNotInitialized_ISE();

    underTest.getRoot();
  }

  @Test
  public void getComponentByRef_throws_ISE_if_root_has_not_been_set() {
    expectNotInitialized_ISE();

    underTest.getComponentByRef(12);
  }

  @Test
  public void getComponentByRef_returns_any_report_component_in_the_tree() {
    underTest.setRoot(SOME_REPORT_COMPONENT_TREE);

    for (int i = 1; i <= 4; i++) {
      assertThat(underTest.getComponentByRef(i).getReportAttributes().getRef()).isEqualTo(i);
    }
  }

  @Test
  public void getComponentByRef_throws_IAE_if_holder_does_not_contain_specified_component() {
    underTest.setRoot(SOME_REPORT_COMPONENT_TREE);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component with ref '6' can't be found");

    underTest.getComponentByRef(6);
  }

  @Test
  public void getComponentByRef_throws_IAE_if_holder_contains_View_tree() {
    underTest.setRoot(SOME_VIEWS_COMPONENT_TREE);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component with ref '1' can't be found");

    underTest.getComponentByRef(1);
  }

  @Test
  public void verify_setRoot_getRoot() {
    underTest.setRoot(DUMB_PROJECT);
    assertThat(underTest.getRoot()).isSameAs(DUMB_PROJECT);
  }

  private void expectNotInitialized_ISE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Holder has not been initialized yet");
  }

}
