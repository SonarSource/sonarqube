/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static org.assertj.core.api.Assertions.assertThat;

public class QGChangeEventTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ComponentDto project = new ComponentDto()
    .setDbKey("foo")
    .setUuid("bar");
  private BranchDto branch = new BranchDto()
    .setBranchType(BranchType.SHORT)
    .setUuid("bar")
    .setProjectUuid("doh")
    .setMergeBranchUuid("zop");
  private SnapshotDto analysis = new SnapshotDto()
    .setUuid("pto")
    .setCreatedAt(8_999_999_765L);
  private Configuration configuration = Mockito.mock(Configuration.class);
  private Metric.Level previousStatus = Metric.Level.values()[new Random().nextInt(Metric.Level.values().length)];
  private Supplier<Optional<EvaluatedQualityGate>> supplier = Optional::empty;

  @Test
  public void constructor_fails_with_NPE_if_project_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("project can't be null");

    new QGChangeEvent(null, branch, analysis, configuration, previousStatus, supplier);
  }

  @Test
  public void constructor_fails_with_NPE_if_branch_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("branch can't be null");

    new QGChangeEvent(project, null, analysis, configuration, previousStatus, supplier);
  }

  @Test
  public void constructor_fails_with_NPE_if_analysis_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("analysis can't be null");

    new QGChangeEvent(project, branch, null, configuration, previousStatus, supplier);
  }

  @Test
  public void constructor_fails_with_NPE_if_configuration_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projectConfiguration can't be null");

    new QGChangeEvent(project, branch, analysis, null, previousStatus, supplier);
  }

  @Test
  public void constructor_does_not_fail_with_NPE_if_previousStatus_is_null() {
    new QGChangeEvent(project, branch, analysis, configuration, null, supplier);
  }

  @Test
  public void constructor_fails_with_NPE_if_supplier_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("qualityGateSupplier can't be null");

    new QGChangeEvent(project, branch, analysis, configuration, previousStatus, null);
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

    assertThat(underTest.toString())
      .isEqualTo("QGChangeEvent{project=bar:foo, branch=SHORT:bar:doh:zop, analysis=pto:8999999765" +
        ", projectConfiguration=" + configuration.toString() +
        ", previousStatus=" + previousStatus +
        ", qualityGateSupplier=" + supplier + "}");

  }
}
