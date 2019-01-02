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
package org.sonar.server.qualitygate;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();

  private QualityGateFinder underTest = new QualityGateFinder(db.getDbClient());

  @Test
  public void return_default_quality_gate_for_project() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto dbQualityGate = db.qualityGates().createDefaultQualityGate(db.getDefaultOrganization(), qg -> qg.setName("Sonar way"));

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, db.getDefaultOrganization(), project);

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGate().getId()).isEqualTo(dbQualityGate.getId());
    assertThat(result.get().isDefault()).isTrue();
  }

  @Test
  public void return_project_quality_gate_over_default() {
    ComponentDto project = db.components().insertPrivateProject();
    db.qualityGates().createDefaultQualityGate(db.getDefaultOrganization(),qg -> qg.setName("Sonar way"));
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization(), qg -> qg.setName("My team QG"));
    db.qualityGates().associateProjectToQualityGate(project, dbQualityGate);

    Optional<QualityGateFinder.QualityGateData> result = underTest.getQualityGate(dbSession, db.getDefaultOrganization(), project);

    assertThat(result).isPresent();
    assertThat(result.get().getQualityGate().getId()).isEqualTo(dbQualityGate.getId());
    assertThat(result.get().isDefault()).isFalse();
  }

  @Test
  public void fail_when_default_qgate_defined_does_not_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto dbQualityGate = db.qualityGates().createDefaultQualityGate(db.getDefaultOrganization(), qg -> qg.setName("Sonar way"));
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);
    db.commit();

    assertThat(underTest.getQualityGate(dbSession, db.getDefaultOrganization(), project)).isEmpty();
  }

  @Test
  public void fail_when_project_qgate_defined_does_not_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization(), qg -> qg.setName("My team QG"));
    db.qualityGates().associateProjectToQualityGate(project, dbQualityGate);
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    assertThat(underTest.getQualityGate(dbSession, db.getDefaultOrganization(), project)).isEmpty();
  }

  @Test
  public void fail_when_default_quality_gate_does_not_exists() {
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization(), qg -> qg.setName("My team QG"));
    db.qualityGates().setDefaultQualityGate(db.getDefaultOrganization(), dbQualityGate);
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Default quality gate [%s] is missing on organization [%s]", dbQualityGate.getUuid(), db.getDefaultOrganization().getUuid()));

    underTest.getDefault(dbSession, db.getDefaultOrganization());
  }

}
