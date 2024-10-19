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
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.measures.Metric.ValueType.STRING;

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
    Consumer<MetricDto> configureMetric = metric -> metric
      .setValueType(STRING.name())
      .setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY);

    MetricDto metric = db.measures().insertMetric(configureMetric);

    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();

    insertLiveMeasure("java", metric).accept(project1);
    insertLiveMeasure("cobol", metric).accept(project2);


    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getValues_when1CppAnd1CProject_returnMapWithSize2AndAutoconfigByDefault() {
    Consumer<MetricDto> configureMetric = metric -> metric
      .setValueType(STRING.name())
      .setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY);

    MetricDto metric = db.measures().insertMetric(configureMetric);

    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    ProjectData project4 = db.components().insertPrivateProject();

    insertLiveMeasure("c", metric).accept(project1);
    insertLiveMeasure("cpp", metric).accept(project2);
    insertLiveMeasure("java", metric).accept(project3);
    insertLiveMeasure("cobol", metric).accept(project4);

    Map<String, String> actualResult = underTest.getValues();

    assertThat(actualResult).hasSize(2)
      .containsExactlyInAnyOrderEntriesOf(
        Map.of(project1.getProjectDto().getUuid(), "AUTOCONFIG", project2.getProjectDto().getUuid(), "AUTOCONFIG")
      );
  }

  @Test
  void getValues_whenCAndCppProjectsWithDifferentConfig_returnMapWithSize2AndNotAutoconfig() {
    Consumer<MetricDto> configureMetric = metric -> metric
      .setValueType(STRING.name())
      .setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY);

    MetricDto metric = db.measures().insertMetric(configureMetric);

    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    ProjectData project4 = db.components().insertPrivateProject();

    insertLiveMeasure("c", metric).accept(project1);
    insertLiveMeasure("cpp", metric).accept(project2);
    insertLiveMeasure("java", metric).accept(project3);
    insertLiveMeasure("cobol", metric).accept(project4);

    db.properties().insertProperty("sonar.cfamily.build-wrapper-output", "anyvalue", project1.getProjectDto().getUuid());
    db.properties().insertProperty("sonar.cfamily.compile-commands", "anyvalue", project2.getProjectDto().getUuid());

    Map<String, String> actualResult = underTest.getValues();

    assertThat(actualResult).hasSize(2)
      .containsExactlyInAnyOrderEntriesOf(
        Map.of(project1.getProjectDto().getUuid(), "BW_DEPRECATED", project2.getProjectDto().getUuid(), "COMPDB")
      );
  }

  private Consumer<LiveMeasureDto> configureLiveMeasure(String language, MetricDto metric, ComponentDto componentDto) {
    return liveMeasure -> liveMeasure
      .setMetricUuid(metric.getUuid())
      .setComponentUuid(componentDto.uuid())
      .setProjectUuid(componentDto.uuid())
      .setData(language + "=" + 100);
  }

  private Consumer<ProjectData> insertLiveMeasure(String language, MetricDto metric) {
    return projectData -> db.measures().insertLiveMeasure(projectData.getMainBranchComponent(), metric,
      configureLiveMeasure(language, metric, projectData.getMainBranchComponent()));
  }
}
