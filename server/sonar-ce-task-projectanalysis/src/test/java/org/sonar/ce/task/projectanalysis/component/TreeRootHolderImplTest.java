/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.DUMB_PROJECT;

public class TreeRootHolderImplTest {

  private static final ReportComponent SOME_REPORT_COMPONENT_TREE = ReportComponent.builder(PROJECT, 1)
    .addChildren(ReportComponent.builder(DIRECTORY, 2)
      .addChildren(
        ReportComponent.builder(FILE, 3).build())
      .build())
    .build();
  private static final ViewsComponent SOME_VIEWS_COMPONENT_TREE = ViewsComponent.builder(VIEW, 1)
    .addChildren(
      ViewsComponent.builder(VIEW, 2)
        .addChildren(ViewsComponent.builder(PROJECT_VIEW, 3).build())
        .build())
    .build();

  private TreeRootHolderImpl underTest = new TreeRootHolderImpl();

  @Test
  public void setRoots_throws_NPE_if_root_is_null() {
    assertThatThrownBy(() -> underTest.setRoots(null, DUMB_PROJECT))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("root can not be null");
  }

  @Test
  public void setRoots_throws_NPE_if_reportRoot_is_null() {
    assertThatThrownBy(() -> underTest.setRoots(DUMB_PROJECT, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("root can not be null");
  }

  @Test
  public void setRoot_throws_ISE_when_called_twice() {
    underTest.setRoots(DUMB_PROJECT, DUMB_PROJECT);

    assertThatThrownBy(() -> underTest.setRoots(null, DUMB_PROJECT))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("root can not be set twice in holder");
  }

  @Test
  public void getRoot_throws_ISE_if_root_has_not_been_set_yet() {
    assertThatThrownBy(() -> underTest.getRoot())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Holder has not been initialized yet");
  }

  @Test
  public void getComponentByRef_throws_ISE_if_root_has_not_been_set() {
    assertThatThrownBy(() -> underTest.getComponentByRef(12))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Holder has not been initialized yet");
  }

  @Test
  public void getComponentByRef_returns_any_report_component_in_the_tree() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);

    for (int i = 1; i <= 3; i++) {
      assertThat(underTest.getComponentByRef(i).getReportAttributes().getRef()).isEqualTo(i);
    }
  }

  @Test
  public void getOptionalComponentByRef_returns_any_report_component_in_the_tree() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);

    for (int i = 1; i <= 3; i++) {
      assertThat(underTest.getOptionalComponentByRef(i).get().getReportAttributes().getRef()).isEqualTo(i);
    }
  }

  @Test
  public void getComponentByRef_throws_IAE_if_holder_does_not_contain_specified_component() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);

    assertThatThrownBy(() -> underTest.getComponentByRef(6))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Component with ref '6' can't be found");
  }

  @Test
  public void getOptionalComponentByRef_returns_empty_if_holder_does_not_contain_specified_component() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);

    assertThat(underTest.getOptionalComponentByRef(6)).isEmpty();
  }

  @Test
  public void getComponentByRef_throws_IAE_if_holder_contains_View_tree() {
    underTest.setRoots(SOME_VIEWS_COMPONENT_TREE, DUMB_PROJECT);

    assertThatThrownBy(() -> underTest.getComponentByRef(1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Component with ref '1' can't be found");
  }

  @Test
  public void verify_setRoots_getRoot() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);
    assertThat(underTest.getRoot()).isSameAs(SOME_REPORT_COMPONENT_TREE);
  }

  @Test
  public void verify_setRoots_getReportRoot() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);
    assertThat(underTest.getReportTreeRoot()).isSameAs(DUMB_PROJECT);
  }

  @Test
  public void getSize_throws_ISE_if_not_initialized() {
    assertThatThrownBy(() -> underTest.getSize())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Holder has not been initialized yet");
  }

  @Test
  public void getSize_counts_number_of_components() {
    underTest.setRoots(SOME_REPORT_COMPONENT_TREE, DUMB_PROJECT);
    assertThat(underTest.getSize()).isEqualTo(3);
  }
}
