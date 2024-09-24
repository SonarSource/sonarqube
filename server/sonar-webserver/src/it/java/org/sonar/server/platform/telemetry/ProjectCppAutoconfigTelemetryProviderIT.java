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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;

class ProjectCppAutoconfigTelemetryProviderIT {

  private final System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @RegisterExtension
  public final DbTester db = DbTester.create(system2);

  ProjectCppAutoconfigTelemetryProvider underTest = new ProjectCppAutoconfigTelemetryProvider(db.getDbClient());

  @Test
  void getValues_whenNoProjects_returnEmptyList() {
    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getValues_whenNoCppAndCProjects_returnEmptyMap() {
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();

    db.measures().insertMeasure(project1, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=100"));
    db.measures().insertMeasure(project2, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "cobol=100"));

    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getValues_when1CppAnd1CProject_returnMapWithSize2AndAutoconfigByDefault() {
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    ProjectData project4 = db.components().insertPrivateProject();

    db.measures().insertMeasure(project1, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "c=100;java=2"));
    db.measures().insertMeasure(project2, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=2;cpp=100"));
    db.measures().insertMeasure(project3, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=100"));
    db.measures().insertMeasure(project4, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "cobol=100"));

    Map<String, String> actualResult = underTest.getValues();

    assertThat(actualResult).hasSize(2)
      .containsExactlyInAnyOrderEntriesOf(
        Map.of(project1.getProjectDto().getUuid(), "AUTOCONFIG", project2.getProjectDto().getUuid(), "AUTOCONFIG")
      );
  }

  @Test
  void getValues_whenCAndCppProjectsWithDifferentConfig_returnMapWithSize2AndNotAutoconfig() {
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    ProjectData project4 = db.components().insertPrivateProject();

    db.measures().insertMeasure(project1, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "c=100"));
    db.measures().insertMeasure(project2, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "cpp=100"));
    db.measures().insertMeasure(project3, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "java=100"));
    db.measures().insertMeasure(project4, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "cobol=100"));

    db.properties().insertProperty("sonar.cfamily.build-wrapper-output", "anyvalue", project1.getProjectDto().getUuid());
    db.properties().insertProperty("sonar.cfamily.compile-commands", "anyvalue", project2.getProjectDto().getUuid());

    Map<String, String> actualResult = underTest.getValues();

    assertThat(actualResult).hasSize(2)
      .containsExactlyInAnyOrderEntriesOf(
        Map.of(project1.getProjectDto().getUuid(), "BW_DEPRECATED", project2.getProjectDto().getUuid(), "COMPDB")
      );
  }
}
