/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateFinder.QualityGateData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class QualityGateFinderIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();

  private final QualityGateFinder underTest = new QualityGateFinder(db.getDbClient());

  @Test
  public void return_default_quality_gate_for_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QualityGateDto dbQualityGate = db.qualityGates().createDefaultQualityGate(qg -> qg.setName("Sonar way"));

    QualityGateData result = underTest.getEffectiveQualityGate(dbSession, project);

    assertThat(result.getUuid()).isEqualTo(dbQualityGate.getUuid());
    assertThat(result.isDefault()).isTrue();
  }

  @Test
  public void return_project_quality_gate_over_default() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.qualityGates().createDefaultQualityGate(qg -> qg.setName("Sonar way"));
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG"));
    db.qualityGates().associateProjectToQualityGate(project, dbQualityGate);

    QualityGateData result = underTest.getEffectiveQualityGate(dbSession, project);

    assertThat(result.getUuid()).isEqualTo(dbQualityGate.getUuid());
    assertThat(result.isDefault()).isFalse();
  }

  @Test
  public void return_quality_gates_for_all_projects() {
    QualityGateDto defaultQualityGate = db.qualityGates().createDefaultQualityGate(qg -> qg.setName("Sonar way"));

    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    QualityGateDto dbQualityGate1 = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG").setAiCodeSupported(true));
    db.qualityGates().associateProjectToQualityGate(project1, dbQualityGate1);

    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    QualityGateDto dbQualityGate2 = db.qualityGates().insertQualityGate(qg -> qg.setName("Another QG").setAiCodeSupported(false));
    db.qualityGates().associateProjectToQualityGate(project2, dbQualityGate2);

    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();

    Map<String, QualityGateData> result = underTest.getEffectiveQualityGateForAllProjects(dbSession);

    assertThat(result).containsOnlyKeys(project1.getUuid(), project2.getUuid(), project3.getUuid())
      .extractingByKeys(project1.getUuid(), project2.getUuid(), project3.getUuid())
      .extracting(QualityGateData::getUuid, QualityGateData::getName, QualityGateData::isBuiltIn, QualityGateData::isDefault, QualityGateData::isAiCodeSupported)
      .containsExactly(
        tuple(dbQualityGate1.getUuid(), dbQualityGate1.getName(), dbQualityGate1.isBuiltIn(), false, dbQualityGate1.isAiCodeSupported()),
        tuple(dbQualityGate2.getUuid(), dbQualityGate2.getName(), dbQualityGate2.isBuiltIn(), false, dbQualityGate2.isAiCodeSupported()),
        tuple(defaultQualityGate.getUuid(), defaultQualityGate.getName(), defaultQualityGate.isBuiltIn(), true, defaultQualityGate.isAiCodeSupported())
      );
  }

  @Test
  public void fail_when_default_qgate_defined_does_not_exist() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QualityGateDto dbQualityGate = db.qualityGates().createDefaultQualityGate(qg -> qg.setName("Sonar way"));
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);
    db.commit();

    assertThatThrownBy(() -> underTest.getEffectiveQualityGate(dbSession, project))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default quality gate is missing");
  }

  @Test
  public void fail_when_project_qgate_defined_does_not_exist() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG"));
    db.qualityGates().setDefaultQualityGate(dbQualityGate);
    db.qualityGates().associateProjectToQualityGate(project, dbQualityGate);
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    assertThatThrownBy(() -> underTest.getEffectiveQualityGate(dbSession, project))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default quality gate is missing");
  }

  @Test
  public void fail_when_qgate_property_does_not_exist() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG"));
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    assertThatThrownBy(() -> underTest.getEffectiveQualityGate(dbSession, project))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default quality gate is missing");
  }

  @Test
  public void fail_when_default_quality_gate_does_not_exists() {
    QualityGateDto dbQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("My team QG"));
    db.qualityGates().setDefaultQualityGate(dbQualityGate);
    db.getDbClient().qualityGateDao().delete(dbQualityGate, dbSession);

    assertThatThrownBy(() -> underTest.getDefault(dbSession))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default quality gate is missing");
  }

}
