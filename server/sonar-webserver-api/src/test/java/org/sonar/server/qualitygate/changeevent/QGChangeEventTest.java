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
package org.sonar.server.qualitygate.changeevent;

import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QGChangeEventTest {

  private final ProjectDto project = new ProjectDto()
    .setKey("foo")
    .setUuid("bar");
  private final BranchDto branch = new BranchDto()
    .setBranchType(BranchType.BRANCH)
    .setUuid("bar")
    .setProjectUuid("doh")
    .setMergeBranchUuid("zop");
  private final SnapshotDto analysis = new SnapshotDto()
    .setUuid("pto")
    .setCreatedAt(8_999_999_765L);
  private final Configuration configuration = Mockito.mock(Configuration.class);
  private final Metric.Level previousStatus = Metric.Level.values()[new Random().nextInt(Metric.Level.values().length)];
  private Supplier<Optional<EvaluatedQualityGate>> supplier = Optional::empty;

  @Test
  public void constructor_fails_with_NPE_if_project_is_null() {
    assertThatThrownBy(() -> new QGChangeEvent(null, branch, analysis, configuration, previousStatus, supplier))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("project can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_branch_is_null() {
    assertThatThrownBy(() -> new QGChangeEvent(project, null, analysis, configuration, previousStatus, supplier))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("branch can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_analysis_is_null() {
    assertThatThrownBy(() -> new QGChangeEvent(project, branch, null, configuration, previousStatus, supplier))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("analysis can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_configuration_is_null() {
    assertThatThrownBy(() -> new QGChangeEvent(project, branch, analysis, null, previousStatus, supplier))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("projectConfiguration can't be null");
  }

  @Test
  public void constructor_does_not_fail_with_NPE_if_previousStatus_is_null() {
    assertThatCode(() -> new QGChangeEvent(project, branch, analysis, configuration, null, supplier)).doesNotThrowAnyException();
  }

  @Test
  public void constructor_fails_with_NPE_if_supplier_is_null() {
    assertThatThrownBy(() -> new QGChangeEvent(project, branch, analysis, configuration, previousStatus, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("qualityGateSupplier can't be null");
  }

  @Test
  public void verify_getters() {
    QGChangeEvent underTest = new QGChangeEvent(project, branch, analysis, configuration, previousStatus, supplier);

    assertThat(underTest.getProject()).isSameAs(project);
    assertThat(underTest.getBranch()).isSameAs(branch);
    assertThat(underTest.getAnalysis()).isSameAs(analysis);
    assertThat(underTest.getProjectConfiguration()).isSameAs(configuration);
    assertThat(underTest.getPreviousStatus()).contains(previousStatus);
    assertThat(underTest.getQualityGateSupplier()).isSameAs(supplier);
  }

  @Test
  public void getPreviousStatus_returns_empty_when_previousStatus_is_null() {
    QGChangeEvent underTest = new QGChangeEvent(project, branch, analysis, configuration, previousStatus, supplier);

    assertThat(underTest.getPreviousStatus()).contains(previousStatus);
  }

  @Test
  public void overrides_toString() {
    QGChangeEvent underTest = new QGChangeEvent(project, branch, analysis, configuration, previousStatus, supplier);

    assertThat(underTest)
      .hasToString("QGChangeEvent{project=bar:foo, branch=BRANCH:bar:doh:zop, analysis=pto:8999999765" +
        ", projectConfiguration=" + configuration.toString() +
        ", previousStatus=" + previousStatus +
        ", qualityGateSupplier=" + supplier + "}");

  }
}
