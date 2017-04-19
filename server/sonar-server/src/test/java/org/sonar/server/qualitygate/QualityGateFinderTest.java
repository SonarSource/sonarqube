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
package org.sonar.server.qualitygate;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();

  private QualityGateFinder underTest = new QualityGateFinder(dbTester.getDbClient());

  @Test
  public void return_default_quality_gate_for_project() {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));
    QualityGateDto dbQualityGate = dbTester.qualityGates().createDefaultQualityGate("Sonar way");

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, project.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGate().getId()).isEqualTo(dbQualityGate.getId());
    assertThat(result.get().isDefault()).isTrue();
  }

  @Test
  public void return_project_quality_gate_over_default() {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()));
    dbTester.qualityGates().createDefaultQualityGate("Sonar way");
    QualityGateDto dbQualityGate = dbTester.qualityGates().insertQualityGate("My team QG");
    dbTester.qualityGates().associateProjectToQualityGate(project, dbQualityGate);

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, project.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGate().getId()).isEqualTo(dbQualityGate.getId());
    assertThat(result.get().isDefault()).isFalse();
  }

  @Test
  public void return_nothing_when_no_default_qgate_and_no_qgate_defined_for_project() {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, project.getId());

    assertThat(result).isNotPresent();
  }

  @Test
  public void fail_when_default_qgate_defined_in_properties_does_not_exists() throws Exception {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()));
    QualityGateDto dbQualityGate = dbTester.qualityGates().createDefaultQualityGate("Sonar way");
    dbTester.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    expectedException.expect(NotFoundException.class);
    underTest.getQualityGate(dbSession, project.getId());
  }

  @Test
  public void fail_when_project_qgate_defined_in_properties_does_not_exists() throws Exception {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));
    QualityGateDto dbQualityGate = dbTester.qualityGates().insertQualityGate("My team QG");
    dbTester.qualityGates().associateProjectToQualityGate(project, dbQualityGate);
    dbTester.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    expectedException.expect(NotFoundException.class);
    underTest.getQualityGate(dbSession, project.getId());
  }
}
