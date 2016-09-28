/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.measure.ws;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.Component;
import org.sonarqube.ws.WsMeasures.SearchWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.measure.ws.SearchAction.PARAM_COMPONENT_IDS;
import static org.sonar.server.measure.ws.SearchAction.PARAM_COMPONENT_KEYS;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  WsActionTester ws = new WsActionTester(new SearchAction(dbClient));

  @Test
  public void json_example() {
    insertJsonExampleData();

    String result = ws.newRequest()
      .setParam(PARAM_COMPONENT_IDS, "project-id,AVIwDXE-bJbJqrw6wFv5,AVIwDXE-bJbJqrw6wFv8,AVIwDXE_bJbJqrw6wFwJ")
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .execute()
      .getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(ws.getDef().responseExampleAsString());

  }

  @Test
  public void project_without_measures_map_all_fields() {
    ComponentDto dbComponent = componentDb.insertComponent(newProjectDto());
    insertComplexityMetric();

    SearchWsResponse result = callByComponentUuids(singletonList(dbComponent.uuid()), singletonList("complexity"));

    assertThat(result.getComponentsCount()).isEqualTo(1);
    Component wsComponent = result.getComponents(0);
    assertThat(wsComponent.getMeasuresCount()).isEqualTo(0);
    assertThat(wsComponent.getId()).isEqualTo(dbComponent.uuid());
    assertThat(wsComponent.getKey()).isEqualTo(dbComponent.key());
    assertThat(wsComponent.getQualifier()).isEqualTo(dbComponent.qualifier());
    assertThat(wsComponent.getName()).isEqualTo(dbComponent.name());
    assertThat(wsComponent.getDescription()).isEqualTo(dbComponent.description());
    assertThat(wsComponent.getProjectId()).isEqualTo("");
    assertThat(wsComponent.getLanguage()).isEqualTo("");
    assertThat(wsComponent.getPath()).isEqualTo("");
    assertThat(wsComponent.getRefId()).isEqualTo("");
    assertThat(wsComponent.getRefKey()).isEqualTo("");
  }

  @Test
  public void search_by_component_key() {
    ComponentDto project = componentDb.insertProject();
    insertComplexityMetric();

    SearchWsResponse result = callByComponentKeys(singletonList(project.key()), singletonList("complexity"));

    assertThat(result.getComponentsCount()).isEqualTo(1);
    assertThat(result.getComponents(0).getId()).isEqualTo(project.uuid());
  }

  @Test
  public void add_best_values_when_no_value() {
    ComponentDto projectDto = newProjectDto("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(projectDto);
    ComponentDto directoryDto = newDirectory(projectDto, "directory-uuid", "path/to/directory").setName("directory-1");
    componentDb.insertComponent(directoryDto);
    ComponentDto file = newFileDto(directoryDto, null, "file-uuid").setName("file-1");
    componentDb.insertComponent(file);
    MetricDto coverage = insertCoverageMetric();
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("ncloc")
      .setValueType(Metric.ValueType.INT.name())
      .setOptimizedBestValue(true)
      .setBestValue(100d)
      .setWorstValue(1000d));
    dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("new_violations")
      .setOptimizedBestValue(true)
      .setBestValue(1984.0d)
      .setValueType(Metric.ValueType.INT.name()));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(coverage, file, projectSnapshot).setValue(15.5d),
      newMeasureDto(coverage, directoryDto, projectSnapshot).setValue(42.0d));
    db.commit();

    SearchWsResponse result = callByComponentUuids(newArrayList(directoryDto.uuid(), file.uuid()), newArrayList("ncloc", "coverage", "new_violations"));

    // directory is not eligible for best value
    assertThat(result.getComponentsList().get(0).getMeasuresList()).extracting("metric").containsOnly("coverage");
    // file measures
    List<WsMeasures.Measure> fileMeasures = result.getComponentsList().get(1).getMeasuresList();
    assertThat(fileMeasures).extracting("metric").containsOnly("ncloc", "coverage", "new_violations");
    assertThat(fileMeasures).extracting("value").containsOnly("100", "15.5", "");
  }

  @Test
  public void fail_if_no_metric() {
    ComponentDto project = componentDb.insertProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'metricKeys' parameter is missing");

    callByComponentUuids(singletonList(project.uuid()), null);
  }

  @Test
  public void fail_if_empty_metric() {
    ComponentDto project = componentDb.insertProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric keys must be provided");

    callByComponentUuids(singletonList(project.uuid()), emptyList());
  }

  @Test
  public void fail_if_unknown_metric() {
    ComponentDto project = componentDb.insertProject();
    insertComplexityMetric();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The following metrics are not found: ncloc, violations");

    callByComponentUuids(singletonList(project.uuid()), newArrayList("violations", "complexity", "ncloc"));
  }

  @Test
  public void fail_if_no_component() {
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either component ids or component keys must be provided, not both");

    call(null, null, singletonList("complexity"));
  }

  @Test
  public void fail_if_empty_component_uuid() {
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either component ids or component keys must be provided, not both");

    callByComponentUuids(emptyList(), singletonList("complexity"));
  }

  @Test
  public void fail_if_empty_component_key() {
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either component ids or component keys must be provided, not both");

    callByComponentKeys(emptyList(), singletonList("complexity"));
  }

  @Test
  public void fail_if_unknown_component_uuid() {
    insertComplexityMetric();
    ComponentDto project = componentDb.insertProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The following component ids are not found: ANOTHER_PROJECT_ID, YOUR_PROJECT_ID");

    callByComponentUuids(newArrayList("YOUR_PROJECT_ID", project.uuid(), "ANOTHER_PROJECT_ID"), singletonList("complexity"));
  }

  @Test
  public void fail_if_unknown_component_key() {
    insertComplexityMetric();
    ComponentDto project = componentDb.insertProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The following component keys are not found: ANOTHER_PROJECT_KEY, YOUR_PROJECT_KEY");

    callByComponentKeys(newArrayList("YOUR_PROJECT_KEY", project.key(), "ANOTHER_PROJECT_KEY"), singletonList("complexity"));
  }

  @Test
  public void fail_if_component_id_and_key() {
    ComponentDto project = componentDb.insertProject();
    ComponentDto anotherProject = componentDb.insertProject();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either component ids or component keys must be provided, not both");

    call(singletonList(project.uuid()), singletonList(anotherProject.key()), singletonList("complexity"));
  }

  @Test
  public void fail_if_more_than_100_component_id() {
    List<String> uuids = IntStream.rangeClosed(1, 101)
      .mapToObj(i -> componentDb.insertProject())
      .map(ComponentDto::uuid)
      .collect(Collectors.toList());
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("101 components provided, more than maximum authorized (100)");

    callByComponentUuids(uuids, singletonList("complexity"));
  }

  @Test
  public void fail_if_more_than_100_component_key() {
    List<String> keys = IntStream.rangeClosed(1, 101)
      .mapToObj(i -> componentDb.insertProject())
      .map(ComponentDto::key)
      .collect(Collectors.toList());
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("101 components provided, more than maximum authorized (100)");

    callByComponentKeys(keys, singletonList("complexity"));
  }

  @Test
  public void definition() {
    WebService.Action result = ws.getDef();

    assertThat(result.key()).isEqualTo("search");
    assertThat(result.isPost()).isFalse();
    assertThat(result.isInternal()).isTrue();
    assertThat(result.since()).isEqualTo("6.1");
    assertThat(result.params()).hasSize(3);
    assertThat(result.responseExampleAsString()).isNotEmpty();
    assertThat(result.description()).isEqualToIgnoringWhitespace("" +
      "Search for component measures ordered by component names.<br>" +
      "At most 100 components can be provided.<br>" +
      "Either 'componentIds' or 'componentKeys' must be provided, not both.<br>" +
      "Requires one of the following permissions:" +
      "<ul>" +
      " <li>'Administer System'</li>" +
      " <li>'Administer' rights on the provided components</li>" +
      " <li>'Browse' on the provided components</li>" +
      "</ul>");
  }

  private SearchWsResponse callByComponentUuids(@Nullable List<String> uuids, @Nullable List<String> metrics) {
    return call(uuids, null, metrics);
  }

  private SearchWsResponse callByComponentKeys(@Nullable List<String> keys, @Nullable List<String> metrics) {
    return call(null, keys, metrics);
  }

  private SearchWsResponse call(@Nullable List<String> uuids, @Nullable List<String> keys, @Nullable List<String> metrics) {
    TestRequest request = ws.newRequest()
      .setMediaType(PROTOBUF);

    if (uuids != null) {
      request.setParam(PARAM_COMPONENT_IDS, String.join(",", uuids));
    }
    if (keys != null) {
      request.setParam(PARAM_COMPONENT_KEYS, String.join(",", keys));
    }
    if (metrics != null) {
      request.setParam(PARAM_METRIC_KEYS, String.join(",", metrics));
    }

    try {
      return SearchWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static MetricDto newMetricDtoWithoutOptimization() {
    return newMetricDto()
      .setWorstValue(null)
      .setBestValue(null)
      .setOptimizedBestValue(false)
      .setUserManaged(false);
  }

  private MetricDto insertNewViolationsMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("new_violations")
      .setShortName("New issues")
      .setDescription("New Issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false)
      .setUserManaged(false)
      .setOptimizedBestValue(true)
      .setBestValue(0.0d));
    db.commit();
    return metric;
  }

  private MetricDto insertNclocMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("ncloc")
      .setShortName("Lines of code")
      .setDescription("Non Commenting Lines of Code")
      .setDomain("Size")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private MetricDto insertComplexityMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("complexity")
      .setShortName("Complexity")
      .setDescription("Cyclomatic complexity")
      .setDomain("Complexity")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private MetricDto insertCoverageMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Code Coverage")
      .setDomain("Coverage")
      .setValueType(Metric.ValueType.FLOAT.name())
      .setDirection(1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private void insertJsonExampleData() {
    ComponentDto project = newProjectDto("project-id")
      .setKey("MY_PROJECT")
      .setName("My Project")
      .setDescription("My Project Description")
      .setQualifier(Qualifiers.PROJECT);
    componentDb.insertComponent(project);
    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodDate(1, parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode(1, "previous_version")
      .setPeriodParam(1, "1.0-SNAPSHOT")
      .setPeriodDate(2, parseDateTime("2016-01-11T10:50:06+0100").getTime())
      .setPeriodMode(2, "previous_analysis")
      .setPeriodParam(2, "2016-01-11")
      .setPeriodDate(3, parseDateTime("2016-01-11T10:38:45+0100").getTime())
      .setPeriodMode(3, "days")
      .setPeriodParam(3, "30"));

    ComponentDto file1 = componentDb.insertComponent(newFileDto(project, null)
      .setUuid("AVIwDXE-bJbJqrw6wFv5")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/ElementImpl.java")
      .setName("ElementImpl.java")
      .setLanguage("java")
      .setQualifier(Qualifiers.FILE)
      .setPath("src/main/java/com/sonarsource/markdown/impl/ElementImpl.java"));
    componentDb.insertComponent(newFileDto(project, null)
      .setUuid("AVIwDXE_bJbJqrw6wFwJ")
      .setKey("com.sonarsource:java-markdown:src/test/java/com/sonarsource/markdown/impl/ElementImplTest.java")
      .setName("ElementImplTest.java")
      .setLanguage("java")
      .setQualifier(Qualifiers.UNIT_TEST_FILE)
      .setPath("src/test/java/com/sonarsource/markdown/impl/ElementImplTest.java"));
    ComponentDto dir = componentDb.insertComponent(newDirectory(project, "src/main/java/com/sonarsource/markdown/impl")
      .setUuid("AVIwDXE-bJbJqrw6wFv8")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(Qualifiers.DIRECTORY));

    MetricDto complexity = insertComplexityMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(complexity, file1, projectSnapshot)
        .setValue(12.0d),
      newMeasureDto(complexity, dir, projectSnapshot)
        .setValue(35.0d)
        .setVariation(2, 0.0d),
      newMeasureDto(complexity, project, projectSnapshot)
        .setValue(42.0d));

    MetricDto ncloc = insertNclocMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file1, projectSnapshot)
        .setValue(114.0d),
      newMeasureDto(ncloc, dir, projectSnapshot)
        .setValue(217.0d)
        .setVariation(2, 0.0d),
      newMeasureDto(ncloc, project, projectSnapshot)
        .setValue(1984.0d));

    MetricDto newViolations = insertNewViolationsMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(newViolations, file1, projectSnapshot)
        .setVariation(1, 25.0d)
        .setVariation(2, 0.0d)
        .setVariation(3, 25.0d),
      newMeasureDto(newViolations, dir, projectSnapshot)
        .setVariation(1, 25.0d)
        .setVariation(2, 0.0d)
        .setVariation(3, 25.0d),
      newMeasureDto(newViolations, project, projectSnapshot)
        .setVariation(1, 255.0d)
        .setVariation(2, 0.0d)
        .setVariation(3, 255.0d));

    db.commit();
  }
}
