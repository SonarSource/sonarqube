/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.measure.ws;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.ws.SearchHistoryAction.SearchHistoryRequest;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.Measures.SearchHistoryResponse;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryMeasure;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryValue;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_FROM;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRICS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_TO;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchHistoryActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final WsActionTester ws = new WsActionTester(new SearchHistoryAction(dbClient, TestComponentFinder.from(db), userSession));

  private ProjectData project;
  private SnapshotDto analysis;
  private MetricDto complexityMetric;
  private MetricDto nclocMetric;
  private MetricDto newViolationMetric;
  private MetricDto stringMetric;
  private MetricDto acceptedIssuesMetric;

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject();
    analysis = db.components().insertSnapshot(project.getProjectDto());
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());
    nclocMetric = insertNclocMetric();
    complexityMetric = insertComplexityMetric();
    newViolationMetric = insertNewViolationMetric();
    stringMetric = insertStringMetric();
    acceptedIssuesMetric = insertAcceptedIssuesMetric();
  }

  @Test
  public void empty_response() {
    project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());
    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(singletonList(complexityMetric.getKey()))
      .build();

    SearchHistoryResponse result = call(request);

    assertThat(result.getMeasuresList()).hasSize(1);
    assertThat(result.getMeasures(0).getHistoryCount()).isZero();

    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      // pagination is applied to the number of analyses
      .containsExactly(1, 100, 0);
  }

  @Test
  public void analyses_but_no_measure() {
    project = db.components().insertPrivateProject();
    analysis = db.components().insertSnapshot(project.getProjectDto());
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(singletonList(complexityMetric.getKey()))
      .build();

    SearchHistoryResponse result = call(request);

    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsExactly(1, 100, 1);
    assertThat(result.getMeasuresList()).hasSize(1);
    assertThat(result.getMeasures(0).getHistoryList()).extracting(HistoryValue::hasDate, HistoryValue::hasValue).containsExactly(tuple(true, false));
  }

  @Test
  public void return_metrics() {
    dbClient.measureDao().insert(dbSession, newMeasureDto(complexityMetric, project.mainBranchUuid(), analysis).setValue(42.0d));
    dbClient.measureDao().insert(dbSession, newMeasureDto(acceptedIssuesMetric, project.mainBranchUuid(), analysis).setValue(10.0d));
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey(), acceptedIssuesMetric.getKey()))
      .build();

    SearchHistoryResponse result = call(request);

    assertThat(result.getMeasuresList()).hasSize(4)
      .extracting(HistoryMeasure::getMetric)
      .containsExactlyInAnyOrder(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey(), acceptedIssuesMetric.getKey());
  }

  @Test
  public void return_renamed_and_deprecated_metric() {
    dbClient.measureDao().insert(dbSession, newMeasureDto(acceptedIssuesMetric, project.mainBranchUuid(), analysis).setValue(10.0d));
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(singletonList("wont_fix_issues"))
      .build();

    SearchHistoryResponse result = call(request);

    assertThat(result.getMeasuresList()).hasSize(1)
      .extracting(HistoryMeasure::getMetric)
      .containsExactlyInAnyOrder("wont_fix_issues");
  }

  @Test
  public void return_measures() {
    SnapshotDto laterAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project.getMainBranchDto()).setCreatedAt(analysis.getCreatedAt() + 42_000));
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(complexityMetric, project.mainBranchUuid(), analysis).setValue(101d),
      newMeasureDto(complexityMetric, project.mainBranchUuid(), laterAnalysis).setValue(100d),
      newMeasureDto(complexityMetric, file, analysis).setValue(42d),
      newMeasureDto(nclocMetric, project.mainBranchUuid(), analysis).setValue(201d),
      newMeasureDto(newViolationMetric, project.mainBranchUuid(), analysis).setValue(5d),
      newMeasureDto(newViolationMetric, project.mainBranchUuid(), laterAnalysis).setValue(10d));
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey()))
      .build();
    SearchHistoryResponse result = call(request);

    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 100, 2);
    assertThat(result.getMeasuresList()).extracting(HistoryMeasure::getMetric).hasSize(3)
      .containsExactly(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey());
    String analysisDate = formatDateTime(analysis.getCreatedAt());
    String laterAnalysisDate = formatDateTime(laterAnalysis.getCreatedAt());
    // complexity measures
    HistoryMeasure complexityMeasures = result.getMeasures(0);
    assertThat(complexityMeasures.getMetric()).isEqualTo(complexityMetric.getKey());
    assertThat(complexityMeasures.getHistoryList()).extracting(HistoryValue::getDate, HistoryValue::getValue)
      .containsExactly(tuple(analysisDate, "101"), tuple(laterAnalysisDate, "100"));
    // ncloc measures
    HistoryMeasure nclocMeasures = result.getMeasures(1);
    assertThat(nclocMeasures.getMetric()).isEqualTo(nclocMetric.getKey());
    assertThat(nclocMeasures.getHistoryList()).extracting(HistoryValue::getDate, HistoryValue::getValue, HistoryValue::hasValue).containsExactly(
      tuple(analysisDate, "201", true), tuple(laterAnalysisDate, "", false));
    // new_violation measures
    HistoryMeasure newViolationMeasures = result.getMeasures(2);
    assertThat(newViolationMeasures.getMetric()).isEqualTo(newViolationMetric.getKey());
    assertThat(newViolationMeasures.getHistoryList()).extracting(HistoryValue::getDate, HistoryValue::getValue)
      .containsExactly(tuple(analysisDate, "5"), tuple(laterAnalysisDate, "10"));
  }

  @Test
  public void pagination_applies_to_analyses() {
    project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());
    List<String> analysisDates = LongStream.rangeClosed(1, 9)
      .mapToObj(i -> dbClient.snapshotDao().insert(dbSession, newAnalysis(project.mainBranchUuid()).setCreatedAt(i * 1_000_000_000)))
      .peek(a -> dbClient.measureDao().insert(dbSession, newMeasureDto(complexityMetric, project.mainBranchUuid(), a).setValue(101d)))
      .map(a -> formatDateTime(a.getCreatedAt()))
      .toList();
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey()))
      .setPage(2)
      .setPageSize(3)
      .build();
    SearchHistoryResponse result = call(request);

    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsExactly(2, 3, 9);
    assertThat(result.getMeasures(0).getHistoryList()).extracting(HistoryValue::getDate).containsExactly(
      analysisDates.get(3), analysisDates.get(4), analysisDates.get(5));
  }

  @Test
  public void inclusive_from_and_to_dates() {
    project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());
    List<String> analysisDates = LongStream.rangeClosed(1, 9)
      .mapToObj(i -> dbClient.snapshotDao().insert(dbSession, newAnalysis(project.mainBranchUuid()).setCreatedAt(System2.INSTANCE.now() + i * 1_000_000_000L)))
      .peek(a -> dbClient.measureDao().insert(dbSession, newMeasureDto(complexityMetric, project.mainBranchUuid(), a).setValue(Double.valueOf(a.getCreatedAt()))))
      .map(a -> formatDateTime(a.getCreatedAt()))
      .toList();
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey()))
      .setFrom(analysisDates.get(1))
      .setTo(analysisDates.get(3))
      .build();
    SearchHistoryResponse result = call(request);

    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsExactly(1, 100, 3);
    assertThat(result.getMeasures(0).getHistoryList()).extracting(HistoryValue::getDate).containsExactly(
      analysisDates.get(1), analysisDates.get(2), analysisDates.get(3));
  }

  @Test
  public void return_best_values_for_files() {
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("optimized").setValueType(ValueType.INT.name()).setOptimizedBestValue(true).setBestValue(456d));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("new_optimized").setValueType(ValueType.INT.name()).setOptimizedBestValue(true).setBestValue(789d));
    db.commit();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(file.getKey())
      .setMetrics(asList("optimized", "new_optimized"))
      .build();
    SearchHistoryResponse result = call(request);

    assertThat(result.getMeasuresCount()).isEqualTo(2);
    assertThat(result.getMeasuresList().get(0).getHistoryList()).extracting(HistoryValue::getValue).containsExactly("789");
    assertThat(result.getMeasuresList().get(1).getHistoryList()).extracting(HistoryValue::getValue).containsExactly("456");

    // Best value is not applied to project
    request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList("optimized", "new_optimized"))
      .build();
    result = call(request);
    assertThat(result.getMeasuresList().get(0).getHistoryCount()).isOne();
    assertThat(result.getMeasuresList().get(0).getHistory(0).hasDate()).isTrue();
    assertThat(result.getMeasuresList().get(0).getHistory(0).hasValue()).isFalse();
  }

  @Test
  public void do_not_return_unprocessed_analyses() {
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project.getMainBranchDto()).setStatus(STATUS_UNPROCESSED));
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey()))
      .build();
    SearchHistoryResponse result = call(request);

    // one analysis in setUp method
    assertThat(result.getPaging().getTotal()).isOne();
  }

  @Test
  public void branch() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setKey("my_branch"));
    userSession.addProjectBranchMapping(project.projectUuid(), branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.mainBranchUuid()));
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    MeasureDto measure = db.measures().insertMeasure(file, analysis, nclocMetric, m -> m.setValue(2d));

    SearchHistoryResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, "my_branch")
      .setParam(PARAM_METRICS, "ncloc")
      .executeProtobuf(SearchHistoryResponse.class);

    assertThat(result.getMeasuresList()).extracting(HistoryMeasure::getMetric).hasSize(1);
    HistoryMeasure historyMeasure = result.getMeasures(0);
    assertThat(historyMeasure.getMetric()).isEqualTo(nclocMetric.getKey());
    assertThat(historyMeasure.getHistoryList())
      .extracting(m -> parseDouble(m.getValue()))
      .containsExactlyInAnyOrder(measure.getValue());
  }

  @Test
  public void pull_request() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setKey("pr-123").setBranchType(PULL_REQUEST));
    userSession.addProjectBranchMapping(project.projectUuid(), branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.mainBranchUuid()));
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    MeasureDto measure = db.measures().insertMeasure(file, analysis, nclocMetric, m -> m.setValue(2d));

    SearchHistoryResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_PULL_REQUEST, "pr-123")
      .setParam(PARAM_METRICS, "ncloc")
      .executeProtobuf(SearchHistoryResponse.class);

    assertThat(result.getMeasuresList()).extracting(HistoryMeasure::getMetric).hasSize(1);
    HistoryMeasure historyMeasure = result.getMeasures(0);
    assertThat(historyMeasure.getMetric()).isEqualTo(nclocMetric.getKey());
    assertThat(historyMeasure.getHistoryList())
      .extracting(m -> parseDouble(m.getValue()))
      .containsExactlyInAnyOrder(measure.getValue());
  }

  @Test
  public void fail_if_unknown_metric() {
    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(asList(complexityMetric.getKey(), nclocMetric.getKey(), "METRIC_42", "42_METRIC"))
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Metrics 42_METRIC, METRIC_42 are not found");
  }

  @Test
  public void fail_if_not_enough_permissions() {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project.getProjectDto());
    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(singletonList(complexityMetric.getKey()))
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_not_enough_permissions_for_application() {
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();

    userSession.logIn()
      .registerApplication(
        application.getProjectDto(),
        project1.getProjectDto(),
        project2.getProjectDto())
      .addProjectPermission(UserRole.USER, application.getProjectDto(), project1.getProjectDto());

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(application.projectKey())
      .setMetrics(singletonList(complexityMetric.getKey()))
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_unknown_component() {
    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent("__UNKNOWN__")
      .setMetrics(singletonList(complexityMetric.getKey()))
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_component_is_removed() {
    ProjectData projectData = db.components().insertPrivateProject();
    db.components().insertComponent(newFileDto(project.getMainBranchComponent()).setKey("file-key").setEnabled(false));
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_COMPONENT, "file-key")
      .setParam(PARAM_METRICS, "ncloc")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Component key 'file-key' not found");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    db.components().insertProjectBranch(project.getProjectDto(), b -> b.setKey("my_branch"));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, "another_branch")
      .setParam(PARAM_METRICS, "ncloc")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search_history");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("6.3");
    assertThat(definition.params()).hasSize(8);

    Param branch = definition.param("branch");
    assertThat(branch.since()).isEqualTo("6.6");
    assertThat(branch.isInternal()).isFalse();
    assertThat(branch.isRequired()).isFalse();
  }

  @Test
  public void json_example() {
    project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());
    long now = parseDateTime("2017-01-23T17:00:53+0100").getTime();
    LongStream.rangeClosed(0, 2)
      .mapToObj(i -> dbClient.snapshotDao().insert(dbSession, newAnalysis(project.getMainBranchDto()).setCreatedAt(now + i * 24 * 1_000 * 60 * 60)))
      .forEach(analysis -> dbClient.measureDao().insert(dbSession,
        newMeasureDto(complexityMetric, project.mainBranchUuid(), analysis).setValue(45d),
        newMeasureDto(newViolationMetric, project.mainBranchUuid(), analysis).setValue(46d),
        newMeasureDto(nclocMetric, project.mainBranchUuid(), analysis).setValue(47d)));
    db.commit();

    String result = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.projectKey())
      .setParam(PARAM_METRICS, String.join(",", asList(complexityMetric.getKey(), nclocMetric.getKey(), newViolationMetric.getKey())))
      .execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void measure_without_values() {
    dbClient.measureDao().insert(dbSession, newMeasureDto(stringMetric, project.mainBranchUuid(), analysis).setValue(null).setData(null));
    db.commit();

    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(project.projectKey())
      .setMetrics(singletonList(stringMetric.getKey()))
      .build();
    SearchHistoryResponse result = call(request);

    HistoryMeasure measure = result.getMeasuresList().stream()
      .filter(m -> m.getMetric().equals(stringMetric.getKey()))
      .findFirst()
      .get();
    assertThat(measure.getHistoryList()).hasSize(1);
    assertThat(measure.getHistory(0).hasValue()).isFalse();
  }

  private SearchHistoryResponse call(SearchHistoryRequest request) {
    TestRequest testRequest = ws.newRequest();

    testRequest.setParam(PARAM_COMPONENT, request.getComponent());
    testRequest.setParam(PARAM_METRICS, String.join(",", request.getMetrics()));
    ofNullable(request.getFrom()).ifPresent(from -> testRequest.setParam(PARAM_FROM, from));
    ofNullable(request.getTo()).ifPresent(to -> testRequest.setParam(PARAM_TO, to));
    ofNullable(request.getPage()).ifPresent(p -> testRequest.setParam(Param.PAGE, String.valueOf(p)));
    ofNullable(request.getPageSize()).ifPresent(ps -> testRequest.setParam(Param.PAGE_SIZE, String.valueOf(ps)));

    return testRequest.executeProtobuf(SearchHistoryResponse.class);
  }

  private static MetricDto newMetricDtoWithoutOptimization() {
    return newMetricDto()
      .setWorstValue(null)
      .setOptimizedBestValue(false)
      .setBestValue(null);
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
      .setHidden(false));
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
      .setHidden(false));
    db.commit();
    return metric;
  }

  private MetricDto insertNewViolationMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("new_violations")
      .setShortName("New issues")
      .setDescription("New Issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false));
    db.commit();
    return metric;
  }

  private MetricDto insertStringMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("a_string")
      .setShortName("A String")
      .setValueType("STRING"));
    db.commit();
    return metric;
  }

  private MetricDto insertAcceptedIssuesMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("accepted_issues")
      .setShortName("Accepted Issues")
      .setValueType("INT"))
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false);
    db.commit();
    return metric;
  }
}
