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
package org.sonar.server.component.ws;

import com.google.common.base.Joiner;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceVerifier;
import org.sonar.server.component.ws.SearchProjectsAction.RequestBuilder;
import org.sonar.server.component.ws.SearchProjectsAction.SearchProjectsRequest;
import org.sonar.server.es.EsTester;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.SearchProjectsWsResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_003;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_QUALIFIER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

@RunWith(DataProviderRunner.class)
public class SearchProjectsActionIT {

  private static final String NCLOC = "ncloc";
  private static final String COVERAGE = "coverage";
  private static final String NEW_COVERAGE = "new_coverage";
  private static final String LEAK_PROJECTS_KEY = "leak_projects";
  private static final String QUALITY_GATE_STATUS = "alert_status";
  private static final String ANALYSIS_DATE = "analysisDate";

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final EsTester es = EsTester.create();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  @DataProvider
  public static Object[][] rating_metric_keys() {
    return new Object[][] {{SQALE_RATING_KEY}, {RELIABILITY_RATING_KEY}, {SECURITY_RATING_KEY}};
  }

  @DataProvider
  public static Object[][] software_quality_rating_metric_keys() {
    return new Object[][] {{SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY}, {SOFTWARE_QUALITY_RELIABILITY_RATING_KEY},
      {SOFTWARE_QUALITY_SECURITY_RATING_KEY}};
  }

  @DataProvider
  public static Object[][] all_rating_metric_keys() {
    List<Object[]> result = new ArrayList<>();
    result.addAll(Arrays.asList(rating_metric_keys()));
    result.addAll(Arrays.asList(software_quality_rating_metric_keys()));
    return result.toArray(new Object[result.size()][]);
  }

  @DataProvider
  public static Object[][] new_rating_metric_keys() {
    return new Object[][] {{NEW_MAINTAINABILITY_RATING_KEY}, {NEW_RELIABILITY_RATING_KEY}, {NEW_SECURITY_RATING_KEY}};
  }

  @DataProvider
  public static Object[][] new_software_quality_rating_metric_keys() {
    return new Object[][] {{NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY}, {NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY},
      {NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY}};
  }

  @DataProvider
  public static Object[][] all_new_rating_metric_keys() {
    List<Object[]> result = new ArrayList<>();
    result.addAll(Arrays.asList(new_rating_metric_keys()));
    result.addAll(Arrays.asList(new_software_quality_rating_metric_keys()));
    return result.toArray(new Object[result.size()][]);
  }

  @DataProvider
  public static Object[][] component_qualifiers_for_valid_editions() {
    return new Object[][] {
      {new String[] {Qualifiers.PROJECT}, Edition.COMMUNITY},
      {new String[] {Qualifiers.APP, Qualifiers.PROJECT}, Edition.DEVELOPER},
      {new String[] {Qualifiers.APP, Qualifiers.PROJECT}, Edition.ENTERPRISE},
      {new String[] {Qualifiers.APP, Qualifiers.PROJECT}, Edition.DATACENTER},
    };
  }

  @DataProvider
  public static Object[][] community_or_developer_edition() {
    return new Object[][] {
      {Edition.COMMUNITY},
      {Edition.DEVELOPER},
    };
  }

  @DataProvider
  public static Object[][] enterprise_or_datacenter_edition() {
    return new Object[][] {
      {Edition.ENTERPRISE},
      {Edition.DATACENTER},
    };
  }

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final PlatformEditionProvider editionProviderMock = mock(PlatformEditionProvider.class);
  private final PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, new ProjectMeasuresIndexer(dbClient,
    es.client()));
  private final ProjectMeasuresIndex index = new ProjectMeasuresIndex(es.client(), new WebAuthorizationTypeSupport(userSession),
    System2.INSTANCE);
  private final ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(db.getDbClient(), es.client());

  private final AiCodeAssuranceVerifier aiCodeAssuranceVerifier = mock(AiCodeAssuranceVerifier.class);

  private final WsActionTester ws = new WsActionTester(new SearchProjectsAction(dbClient, index, userSession, editionProviderMock, aiCodeAssuranceVerifier));

  private final RequestBuilder request = SearchProjectsRequest.builder();

  @Test
  public void verify_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.key()).isEqualTo("search_projects");
    assertThat(def.since()).isEqualTo("6.2");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params().stream().map(Param::key).toList()).containsOnly("filter", "facets", "s", "asc", "ps", "p", "f");
    assertThat(def.changelog()).hasSize(5);

    Param sort = def.param("s");
    assertThat(sort.defaultValue()).isEqualTo("name");
    assertThat(sort.possibleValues()).containsExactlyInAnyOrder(
      "coverage",
      "reliability_rating",
      "duplicated_lines_density",
      "ncloc_language_distribution",
      "lines",
      "new_lines",
      "security_rating",
      "security_review_rating",
      "new_security_review_rating",
      "security_hotspots_reviewed",
      "new_security_hotspots_reviewed",
      "new_reliability_rating",
      "new_coverage",
      "new_security_rating",
      "sqale_rating",
      "new_duplicated_lines_density",
      "alert_status",
      "ncloc",
      "new_maintainability_rating",
      "name",
      "analysisDate",
      "creationDate",
      "new_software_quality_maintainability_rating",
      "new_software_quality_reliability_rating",
      "new_software_quality_security_rating",
      "software_quality_maintainability_rating",
      "software_quality_reliability_rating",
      "software_quality_security_rating");

    Param asc = def.param("asc");
    assertThat(asc.defaultValue()).isEqualTo("true");
    assertThat(asc.possibleValues()).containsOnly("true", "false", "yes", "no");

    Param f = def.param("f");
    assertThat(f.defaultValue()).isNull();
    assertThat(f.possibleValues()).containsOnly("_all", "analysisDate", "leakPeriodDate");

    Param facets = def.param("facets");
    assertThat(facets.defaultValue()).isNull();
    assertThat(facets.possibleValues()).containsOnly("ncloc", "duplicated_lines_density", "coverage", "sqale_rating", "reliability_rating", "security_rating", "alert_status",
      "languages", "tags", "qualifier", "new_reliability_rating", "new_security_rating", "new_maintainability_rating", "new_coverage",
      "new_duplicated_lines_density", "new_lines",
      "security_review_rating", "security_hotspots_reviewed", "new_security_hotspots_reviewed", "new_security_review_rating",
      "new_software_quality_maintainability_rating",
      "new_software_quality_reliability_rating",
      "new_software_quality_security_rating",
      "software_quality_maintainability_rating",
      "software_quality_reliability_rating",
      "software_quality_security_rating");
  }

  @Test
  public void json_example() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(
      c -> c.setKey(KEY_PROJECT_EXAMPLE_001).setName("My Project 1"),
      p -> p.setTagsString("finance, java"),
      new Measure(coverage, c -> c.setValue(80d)));

    ComponentDto project2 = insertProject(
      c -> c.setKey(KEY_PROJECT_EXAMPLE_002).setName("My Project 2"),
      new Measure(coverage, c -> c.setValue(90d)));
    ComponentDto project3 = insertProject(
      c -> c.setKey(KEY_PROJECT_EXAMPLE_003).setName("My Project 3"),
      p -> p.setTagsString("sales, offshore, java"),
      new Measure(coverage, c -> c.setValue(20d)));
    addFavourite(db.components().getProjectDtoByMainBranch(project1));
    index();

    String jsonResult = ws.newRequest()
      .setParam(FACETS, COVERAGE)
      .setParam(FIELDS, "_all")
      .execute().getInput();

    assertJson(jsonResult).ignoreFields("id").isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).ignoreFields("id").isSimilarTo(jsonResult);

    SearchProjectsWsResponse protobufResult = ws.newRequest()
      .setParam(FACETS, COVERAGE)
      .executeProtobuf(SearchProjectsWsResponse.class);

    assertThat(protobufResult.getComponentsList()).extracting(Component::getKey)
      .containsExactly(project1.getKey(), project2.getKey(), project3.getKey());
  }

  @Test
  public void order_by_name_case_insensitive() {
    userSession.logIn();
    insertProject(c -> c.setName("Maven"));
    insertProject(c -> c.setName("Apache"));
    insertProject(c -> c.setName("guava"));
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName)
      .containsExactly("Apache", "guava", "Maven");
  }

  @Test
  public void paginate_result() {
    userSession.logIn();
    IntStream.rangeClosed(1, 9).forEach(i -> insertProject(c -> c.setName("PROJECT-" + i)));
    index();

    SearchProjectsWsResponse result = call(request.setPage(2).setPageSize(3));

    assertThat(result.getPaging().getPageIndex()).isEqualTo(2);
    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
    assertThat(result.getPaging().getTotal()).isEqualTo(9);
    assertThat(result.getComponentsCount()).isEqualTo(3);
    assertThat(result.getComponentsList())
      .extracting(Component::getName)
      .containsExactly("PROJECT-4", "PROJECT-5", "PROJECT-6");
  }

  @Test
  public void empty_result() {
    userSession.logIn();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isZero();
    Common.Paging paging = result.getPaging();
    assertThat(paging.getPageIndex()).isOne();
    assertThat(paging.getPageSize()).isEqualTo(100);
    assertThat(paging.getTotal()).isZero();
  }

  @Test
  public void filter_projects_with_query() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType(INT.name()));
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    ComponentDto project1 = insertProject(
      new Measure(coverage, c -> c.setValue(81d)),
      new Measure(ncloc, c -> c.setValue(10_000d)));
    ComponentDto project2 = insertProject(
      new Measure(coverage, c -> c.setValue(80d)),
      new Measure(ncloc, c -> c.setValue(10_000d)));
    ComponentDto project3 = insertProject(
      new Measure(coverage, c -> c.setValue(80d)),
      new Measure(ncloc, c -> c.setValue(10_001d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("coverage <= 80 and ncloc <= 10000"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(project2.getKey());
  }

  @Test
  public void filter_projects_by_quality_gate() {
    userSession.logIn();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(QUALITY_GATE_STATUS).setValueType(LEVEL.name()));
    ComponentDto project1 = insertProject(new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));
    ComponentDto project2 = insertProject(new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));
    ComponentDto project3 = insertProject(new Measure(qualityGateStatus, c -> c.setValue(null).setData("ERROR")));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("alert_status = OK"));

    assertThat(result.getComponentsList())
      .extracting(Component::getKey)
      .containsExactlyInAnyOrder(project1.getKey(), project2.getKey());
  }

  @Test
  public void filter_projects_by_languages() {
    userSession.logIn();
    MetricDto nclocMetric = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    MetricDto languagesDistributionMetric = db.measures().insertMetric(c -> c.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY).setValueType("DATA"));
    ComponentDto project1 = insertProject(new Measure(languagesDistributionMetric,
      c -> c.setValue(null).setData("<null>=2;java=6;xoo=18")));
    db.measures().insertLiveMeasure(project1, nclocMetric, m -> m.setValue(26d));
    ComponentDto project2 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("java=3;xoo=9")));
    db.measures().insertLiveMeasure(project2, nclocMetric, m -> m.setValue(12d));
    ComponentDto project3 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("xoo=1")));
    db.measures().insertLiveMeasure(project3, nclocMetric, m -> m.setValue(1d));
    ComponentDto project4 = insertProject(new Measure(languagesDistributionMetric,
      c -> c.setValue(null).setData("<null>=1;java=5;xoo=13")));
    db.measures().insertLiveMeasure(project4, nclocMetric, m -> m.setValue(19d));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("languages IN (java, js, <null>)"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project2.getKey(),
      project4.getKey());
  }

  @Test
  @UseDataProvider("all_rating_metric_keys")
  public void filter_projects_by_rating(String metricKey) {
    userSession.logIn();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(metricKey).setValueType(INT.name()));
    ComponentDto project1 = insertProject(new Measure(ratingMetric, c -> c.setValue(1d)));
    ComponentDto project2 = insertProject(new Measure(ratingMetric, c -> c.setValue(2d)));
    ComponentDto project3 = insertProject(new Measure(ratingMetric, c -> c.setValue(3d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter(metricKey + " = 2"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(project2.getKey());
  }

  @Test
  @UseDataProvider("all_new_rating_metric_keys")
  public void filter_projects_by_new_rating(String newMetricKey) {
    userSession.logIn();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(newMetricKey).setValueType(INT.name()));
    insertProject(new Measure(ratingMetric, c -> c.setValue(1d)));
    ComponentDto project2 = insertProject(new Measure(ratingMetric, c -> c.setValue(2d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(3d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter(newMetricKey + " = 2"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(project2.getKey());
  }

  @Test
  public void filter_projects_by_tags() {
    userSession.logIn();
    ComponentDto project1 = insertProject(defaults(), p -> p.setTags(asList("finance", "platform")));
    insertProject(defaults(), p -> p.setTags(singletonList("marketing")));
    ComponentDto project3 = insertProject(defaults(), p -> p.setTags(singletonList("offshore")));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("tags in (finance, offshore)"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_coverage() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(new Measure(coverage, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(new Measure(coverage, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(new Measure(coverage, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("coverage <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_new_coverage() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_COVERAGE).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(new Measure(coverage, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(new Measure(coverage, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(new Measure(coverage, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("new_coverage <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_duplications() {
    userSession.logIn();
    MetricDto duplications = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(new Measure(duplications, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(new Measure(duplications, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(new Measure(duplications, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("duplicated_lines_density <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_no_duplication() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    MetricDto duplications = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(new Measure(coverage, c -> c.setValue(10d)));
    ComponentDto project2 = insertProject(new Measure(duplications, c -> c.setValue(0d)));
    ComponentDto project3 = insertProject(new Measure(duplications, c -> c.setValue(79d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("duplicated_lines_density = NO_DATA"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey());
  }

  @Test
  public void filter_projects_by_no_duplication_should_not_return_projects_with_duplication() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    MetricDto duplications = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    insertProject(new Measure(duplications, c -> c.setValue(10d)), new Measure(coverage, c -> c.setValue(50d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("duplicated_lines_density = NO_DATA"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).isEmpty();
  }

  @Test
  public void filter_projects_by_new_duplications() {
    userSession.logIn();
    MetricDto newDuplications = db.measures().insertMetric(c -> c.setKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(new Measure(newDuplications, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(new Measure(newDuplications, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(new Measure(newDuplications, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("new_duplicated_lines_density <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_ncloc() {
    userSession.logIn();
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    ComponentDto project1 = insertProject(new Measure(ncloc, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(new Measure(ncloc, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(new Measure(ncloc, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("ncloc <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_new_lines() {
    userSession.logIn();
    MetricDto newLines = db.measures().insertMetric(c -> c.setKey(NEW_LINES_KEY).setValueType(INT.name()));
    ComponentDto project1 = insertProject(new Measure(newLines, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(new Measure(newLines, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(new Measure(newLines, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("new_lines <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getKey(), project3.getKey());
  }

  @Test
  public void filter_projects_by_text_query() {
    userSession.logIn();
    insertProject(c -> c.setKey("sonar-java").setName("Sonar Java"));
    insertProject(c -> c.setKey("sonar-groovy").setName("Sonar Groovy"));
    insertProject(c -> c.setKey("sonar-markdown").setName("Sonar Markdown"));
    insertProject(c -> c.setKey("sonarqube").setName("Sonar Qube"));
    index();

    assertThat(call(request.setFilter("query = \"Groovy\"")).getComponentsList()).extracting(Component::getName).containsOnly("Sonar " +
      "Groovy");
    assertThat(call(request.setFilter("query = \"oNar\"")).getComponentsList()).extracting(Component::getName).containsOnly("Sonar Java",
      "Sonar Groovy", "Sonar Markdown",
      "Sonar Qube");
    assertThat(call(request.setFilter("query = \"sonar-java\"")).getComponentsList()).extracting(Component::getName).containsOnly("Sonar " +
      "Java");
  }

  @Test
  public void filter_projects_on_favorites() {
    userSession.logIn();
    ComponentDto javaProject = insertProject();
    ComponentDto markDownProject = insertProject();
    ComponentDto sonarQubeProject = insertProject();
    Stream.of(javaProject, markDownProject).forEach(c -> addFavourite(db.components().getProjectDtoByMainBranch(c)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("isFavorite"));

    assertThat(result.getComponentsCount()).isEqualTo(2);
    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(javaProject.getKey(), markDownProject.getKey());
  }

  @Test
  public void does_not_fail_on_orphan_favorite() {
    userSession.logIn();
    ComponentDto javaProject = insertProject();
    ComponentDto markDownProject = insertProject();
    ComponentDto sonarQubeProject = insertProject();
    Stream.of(javaProject, markDownProject).forEach(c -> addFavourite(db.components().getProjectDtoByMainBranch(c)));
    index();

    addFavourite(null, null, null, null);

    SearchProjectsWsResponse result = call(request.setFilter("isFavorite"));

    assertThat(result.getComponentsCount()).isEqualTo(2);
    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(javaProject.getKey(), markDownProject.getKey());
  }

  @Test
  public void filtering_on_favorites_returns_empty_results_if_not_logged_in() {
    userSession.anonymous();
    ComponentDto javaProject = insertProject();
    ComponentDto markDownProject = insertProject();
    ComponentDto sonarQubeProject = insertProject();
    Stream.of(javaProject, markDownProject).forEach(c -> addFavourite(db.components().getProjectDtoByMainBranch(c)));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("isFavorite"));

    assertThat(result.getComponentsCount()).isZero();
  }

  @Test
  @UseDataProvider("component_qualifiers_for_valid_editions")
  public void default_filter_projects_and_apps_by_editions(String[] qualifiers, Edition edition) {
    when(editionProviderMock.get()).thenReturn(Optional.of(edition));
    userSession.logIn();
    ComponentDto portfolio1 = insertPortfolio();
    ComponentDto portfolio2 = insertPortfolio();

    ComponentDto application1 = insertApplication();
    ComponentDto application2 = insertApplication();
    ComponentDto application3 = insertApplication();

    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto project3 = insertProject();
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(
      Stream.of(application1, application2, application3, project1, project2, project3)
        .filter(c -> Stream.of(qualifiers).anyMatch(s -> s.equals(c.qualifier())))
        .count());

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactly(
        Stream.of(application1, application2, application3, project1, project2, project3)
          .filter(c -> Stream.of(qualifiers).anyMatch(s -> s.equals(c.qualifier())))
          .map(ComponentDto::getKey)
          .toArray(String[]::new));
  }

  @Test
  public void should_return_projects_only_when_no_edition() {
    when(editionProviderMock.get()).thenReturn(Optional.empty());
    userSession.logIn();

    ComponentDto portfolio1 = insertPortfolio();
    ComponentDto portfolio2 = insertPortfolio();

    insertApplication();
    insertApplication();
    insertApplication();

    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto project3 = insertProject();
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(3);

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactly(Stream.of(project1, project2, project3).map(ComponentDto::getKey).toArray(String[]::new));
  }

  @Test
  @UseDataProvider("enterprise_or_datacenter_edition")
  public void filter_projects_and_apps_by_APP_qualifier_when_ee_dc(Edition edition) {
    when(editionProviderMock.get()).thenReturn(Optional.of(edition));
    userSession.logIn();
    ComponentDto application1 = insertApplication();
    ComponentDto application2 = insertApplication();
    ComponentDto application3 = insertApplication();

    insertProject();
    insertProject();
    insertProject();
    index();

    SearchProjectsWsResponse result = call(request.setFilter("qualifier = APP"));

    assertThat(result.getComponentsCount())
      .isEqualTo(3);

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactly(
        Stream.of(application1, application2, application3)
          .map(ComponentDto::getKey)
          .toArray(String[]::new));
  }

  @Test
  @UseDataProvider("enterprise_or_datacenter_edition")
  public void filter_projects_and_apps_by_TRK_qualifier_when_ee_or_dc(Edition edition) {
    when(editionProviderMock.get()).thenReturn(Optional.of(edition));
    userSession.logIn();

    insertApplication();
    insertApplication();
    insertApplication();

    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto project3 = insertProject();
    index();

    SearchProjectsWsResponse result = call(request.setFilter("qualifier = TRK"));

    assertThat(result.getComponentsCount())
      .isEqualTo(3);

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactly(
        Stream.of(project1, project2, project3)
          .map(ComponentDto::getKey)
          .toArray(String[]::new));
  }

  @Test
  @UseDataProvider("community_or_developer_edition")
  public void fail_when_qualifier_filter_by_APP_set_when_ce_or_de(Edition edition) {
    when(editionProviderMock.get()).thenReturn(Optional.of(edition));
    userSession.logIn();

    assertThatThrownBy(() -> call(request.setFilter("qualifiers = APP")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @UseDataProvider("enterprise_or_datacenter_edition")
  public void fail_when_qualifier_filter_invalid_when_ee_or_dc(Edition edition) {
    when(editionProviderMock.get()).thenReturn(Optional.of(edition));
    userSession.logIn();

    assertThatThrownBy(() -> call(request.setFilter("qualifiers = BLA")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void do_not_return_isFavorite_if_anonymous_user() {
    userSession.anonymous();
    insertProject();
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::hasIsFavorite).containsExactlyInAnyOrder(false);
  }

  @Test
  public void return_nloc_facet() {
    userSession.logIn();
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    insertProject(new Measure(ncloc, c -> c.setValue(5d)));
    insertProject(new Measure(ncloc, c -> c.setValue(5d)));
    insertProject(new Measure(ncloc, c -> c.setValue(10_000d)));
    insertProject(new Measure(ncloc, c -> c.setValue(500_001d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(NCLOC)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> NCLOC.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("*-1000.0", 2L),
        tuple("1000.0-10000.0", 0L),
        tuple("10000.0-100000.0", 1L),
        tuple("100000.0-500000.0", 0L),
        tuple("500000.0-*", 1L));
  }

  @Test
  public void return_new_lines_facet() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_LINES_KEY).setValueType(INT.name()));
    insertProject(new Measure(coverage, c -> c.setValue(100d)));
    insertProject(new Measure(coverage, c -> c.setValue(15_000d)));
    insertProject(new Measure(coverage, c -> c.setValue(50_000d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(NEW_LINES_KEY)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> NEW_LINES_KEY.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("*-1000.0", 1L),
        tuple("1000.0-10000.0", 0L),
        tuple("10000.0-100000.0", 2L),
        tuple("100000.0-500000.0", 0L),
        tuple("500000.0-*", 0L));
  }

  @Test
  public void return_languages_facet() {
    userSession.logIn();
    MetricDto nclocMetric = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    MetricDto languagesDistributionMetric = db.measures().insertMetric(c -> c.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY).setValueType("DATA"));
    ComponentDto project1 = insertProject(new Measure(languagesDistributionMetric,
      c -> c.setValue(null).setData("<null>=2;java=6;xoo=18")));
    db.measures().insertLiveMeasure(project1, nclocMetric, m -> m.setValue(26d));
    ComponentDto project2 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("java=5;xoo=19")));
    db.measures().insertLiveMeasure(project2, nclocMetric, m -> m.setValue(24d));
    ComponentDto project3 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("xoo=1")));
    db.measures().insertLiveMeasure(project3, nclocMetric, m -> m.setValue(1d));
    ComponentDto project4 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("<null>=1;java=3;xoo=8")));
    db.measures().insertLiveMeasure(project4, nclocMetric, m -> m.setValue(12d));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(FILTER_LANGUAGES)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_LANGUAGES.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("xoo", 4L),
        tuple("java", 3L),
        tuple("<null>", 2L));
  }

  @Test
  public void return_languages_facet_with_language_having_no_project_if_language_is_in_filter() {
    userSession.logIn();
    MetricDto languagesDistributionMetric = db.measures().insertMetric(c -> c.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY).setValueType("DATA"));
    MetricDto nclocMetric = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    ComponentDto project1 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("<null>=2;java=6")));
    db.measures().insertLiveMeasure(project1, nclocMetric, m -> m.setValue(8d));
    ComponentDto project2 = insertProject(new Measure(languagesDistributionMetric, c -> c.setValue(null).setData("java=5")));
    db.measures().insertLiveMeasure(project2, nclocMetric, m -> m.setValue(5d));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("languages = xoo").setFacets(singletonList(FILTER_LANGUAGES)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_LANGUAGES.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("xoo", 0L),
        tuple("java", 2L),
        tuple("<null>", 1L));
  }

  @Test
  public void return_tags_facet() {
    userSession.logIn();
    insertProject(defaults(), p -> p.setTags(asList("finance", "platform")));
    insertProject(defaults(), p -> p.setTags(singletonList("offshore")));
    insertProject(defaults(), p -> p.setTags(singletonList("offshore")));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(FILTER_TAGS)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_TAGS.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("offshore", 2L),
        tuple("finance", 1L),
        tuple("platform", 1L));
  }

  @Test
  public void return_tags_facet_with_tags_having_no_project_if_tags_is_in_filter() {
    userSession.logIn();
    insertProject(defaults(), p -> p.setTags(asList("finance", "platform")));
    insertProject(defaults(), p -> p.setTags(singletonList("offshore")));
    insertProject(defaults(), p -> p.setTags(singletonList("offshore")));
    index();

    SearchProjectsWsResponse result = call(request.setFilter("tags = marketing").setFacets(singletonList(FILTER_TAGS)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_TAGS.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("offshore", 2L),
        tuple("finance", 1L),
        tuple("platform", 1L),
        tuple("marketing", 0L));
  }

  @Test
  public void return_qualifiers_facet() {
    when(editionProviderMock.get()).thenReturn(Optional.of(Edition.ENTERPRISE));
    userSession.logIn();
    ComponentDto application1 = insertApplication();
    ComponentDto application2 = insertApplication();
    ComponentDto application3 = insertApplication();
    ComponentDto application4 = insertApplication();

    ComponentDto project1 = insertProject();
    ComponentDto project2 = insertProject();
    ComponentDto project3 = insertProject();
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(FILTER_QUALIFIER)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_QUALIFIER.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("APP", 4L),
        tuple("TRK", 3L));
  }

  @Test
  public void return_qualifiers_facet_with_qualifiers_having_no_project_if_qualifiers_is_in_filter() {
    when(editionProviderMock.get()).thenReturn(Optional.of(Edition.ENTERPRISE));
    userSession.logIn();
    ComponentDto application1 = insertApplication();
    ComponentDto application2 = insertApplication();
    ComponentDto application3 = insertApplication();
    ComponentDto application4 = insertApplication();
    index();

    SearchProjectsWsResponse result = call(request.setFilter("qualifier = APP").setFacets(singletonList(FILTER_QUALIFIER)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_QUALIFIER.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("APP", 4L),
        tuple("TRK", 0L));
  }

  @Test
  @UseDataProvider("rating_metric_keys")
  public void return_rating_facet(String ratingMetricKey) {
    userSession.logIn();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(ratingMetricKey).setValueType("RATING"));
    insertProject(new Measure(ratingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(3d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(5d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(ratingMetricKey)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> ratingMetricKey.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("1", 2L),
        tuple("2", 0L),
        tuple("3", 1L),
        tuple("4", 0L),
        tuple("5", 1L));
  }

  @Test
  @UseDataProvider("software_quality_rating_metric_keys")
  public void return_software_quality_rating_facet(String ratingMetricKey) {
    userSession.logIn();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(ratingMetricKey).setValueType("RATING"));
    insertProject(new Measure(ratingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(3d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(5d)));
    insertProject(new Measure(ratingMetric, c -> c.setValue(5d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(ratingMetricKey)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> ratingMetricKey.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("1", 2L),
        tuple("2", 0L),
        tuple("3", 1L),
        tuple("4", 0L),
        tuple("5", 2L));
  }

  @Test
  @UseDataProvider("new_rating_metric_keys")
  public void return_new_rating_facet(String newRatingMetricKey) {
    userSession.logIn();
    MetricDto newRatingMetric = db.measures().insertMetric(c -> c.setKey(newRatingMetricKey).setValueType("RATING"));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(3d)));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(5d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(newRatingMetricKey)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> newRatingMetricKey.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("1", 2L),
        tuple("2", 0L),
        tuple("3", 1L),
        tuple("4", 0L),
        tuple("5", 1L));
  }

  @Test
  @UseDataProvider("new_software_quality_rating_metric_keys")
  public void return_new_software_quality_rating_facet(String newRatingMetricKey) {
    userSession.logIn();
    MetricDto newRatingMetric = db.measures().insertMetric(c -> c.setKey(newRatingMetricKey).setValueType("RATING"));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(1d)));
    insertProject(new Measure(newRatingMetric, c -> c.setValue(3d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(newRatingMetricKey)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> newRatingMetricKey.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("1", 2L),
        tuple("2", 0L),
        tuple("3", 1L),
        tuple("4", 0L),
        tuple("5", 0L));
  }

  @Test
  public void return_coverage_facet() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    insertProject();
    insertProject(new Measure(coverage, c -> c.setValue(80d)));
    insertProject(new Measure(coverage, c -> c.setValue(85d)));
    insertProject(new Measure(coverage, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(COVERAGE)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> COVERAGE.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("NO_DATA", 1L),
        tuple("*-30.0", 1L),
        tuple("30.0-50.0", 0L),
        tuple("50.0-70.0", 0L),
        tuple("70.0-80.0", 0L),
        tuple("80.0-*", 2L));
  }

  @Test
  public void return_new_coverage_facet() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_COVERAGE).setValueType("PERCENT"));
    insertProject();
    insertProject(new Measure(coverage, c -> c.setValue(80d)));
    insertProject(new Measure(coverage, c -> c.setValue(85d)));
    insertProject(new Measure(coverage, c -> c.setValue(10d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(NEW_COVERAGE)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> NEW_COVERAGE.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("NO_DATA", 1L),
        tuple("*-30.0", 1L),
        tuple("30.0-50.0", 0L),
        tuple("50.0-70.0", 0L),
        tuple("70.0-80.0", 0L),
        tuple("80.0-*", 2L));
  }

  @Test
  public void return_duplications_facet() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    insertProject(new Measure(coverage, c -> c.setValue(10d)));
    insertProject(new Measure(coverage, c -> c.setValue(15d)));
    insertProject(new Measure(coverage, c -> c.setValue(5d)));
    insertProject();
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(DUPLICATED_LINES_DENSITY_KEY)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> DUPLICATED_LINES_DENSITY_KEY.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("NO_DATA", 1L),
        tuple("*-3.0", 0L),
        tuple("3.0-5.0", 0L),
        tuple("5.0-10.0", 1L),
        tuple("10.0-20.0", 2L),
        tuple("20.0-*", 0L));
  }

  @Test
  public void return_new_duplications_facet() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    insertProject();
    insertProject(new Measure(coverage, c -> c.setValue(10d)));
    insertProject(new Measure(coverage, c -> c.setValue(15d)));
    insertProject(new Measure(coverage, c -> c.setValue(5d)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(NEW_DUPLICATED_LINES_DENSITY_KEY)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> NEW_DUPLICATED_LINES_DENSITY_KEY.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("NO_DATA", 1L),
        tuple("*-3.0", 0L),
        tuple("3.0-5.0", 0L),
        tuple("5.0-10.0", 1L),
        tuple("10.0-20.0", 2L),
        tuple("20.0-*", 0L));
  }

  @Test
  public void return_quality_gate_facet() {
    userSession.logIn();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(ALERT_STATUS_KEY).setValueType(LEVEL.name()));
    insertProject(new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(new Measure(qualityGateStatus, c -> c.setData(Metric.Level.OK.name()).setValue(null)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(ALERT_STATUS_KEY)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> ALERT_STATUS_KEY.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("OK", 1L),
        tuple("ERROR", 2L));
  }

  @Test
  public void return_quality_gate_facet_without_warning_when_no_projects_in_warning() {
    userSession.logIn();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(ALERT_STATUS_KEY).setValueType(LEVEL.name()));
    insertProject(new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(new Measure(qualityGateStatus, c -> c.setData(Metric.Level.OK.name()).setValue(null)));
    index();

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(ALERT_STATUS_KEY)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> ALERT_STATUS_KEY.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("OK", 1L),
        tuple("ERROR", 2L));
  }

  @Test
  public void default_sort_is_by_ascending_name() {
    userSession.logIn();
    insertProject(c -> c.setName("Sonar Java"));
    insertProject(c -> c.setName("Sonar Groovy"));
    insertProject(c -> c.setName("Sonar Markdown"));
    insertProject(c -> c.setName("Sonar Qube"));
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName).containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown",
      "Sonar Qube");
  }

  @Test
  public void sort_by_name() {
    userSession.logIn();
    insertProject(c -> c.setName("Sonar Java"));
    insertProject(c -> c.setName("Sonar Groovy"));
    insertProject(c -> c.setName("Sonar Markdown"));
    insertProject(c -> c.setName("Sonar Qube"));
    index();

    assertThat(call(request.setSort("name").setAsc(true)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown", "Sonar Qube");
    assertThat(call(request.setSort("name").setAsc(false)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Qube", "Sonar Markdown", "Sonar Java", "Sonar Groovy");
  }

  @Test
  public void sort_by_coverage_then_by_name() {
    userSession.logIn();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType(INT.name()));
    ComponentDto project1 = insertProject(c -> c.setName("Sonar Java"), new Measure(coverage, c -> c.setValue(81d)));
    ComponentDto project2 = insertProject(c -> c.setName("Sonar Groovy"), new Measure(coverage, c -> c.setValue(81d)));
    ComponentDto project3 = insertProject(c -> c.setName("Sonar Markdown"), new Measure(coverage, c -> c.setValue(80d)));
    ComponentDto project4 = insertProject(c -> c.setName("Sonar Qube"), new Measure(coverage, c -> c.setValue(80d)));
    index();

    assertThat(call(request.setSort(COVERAGE).setAsc(true)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project3.getKey(), project4.getKey(), project2.getKey(), project1.getKey());
    assertThat(call(request.setSort(COVERAGE).setAsc(false)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project2.getKey(), project1.getKey(), project3.getKey(), project4.getKey());
  }

  @Test
  public void sort_by_quality_gate_then_by_name() {
    userSession.logIn();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(QUALITY_GATE_STATUS).setValueType(LEVEL.name()));
    ComponentDto project1 = insertProject(c -> c.setName("Sonar Java"), new Measure(qualityGateStatus, c -> c.setValue(null).setData(
      "ERROR")));
    ComponentDto project2 = insertProject(c -> c.setName("Sonar Groovy"), new Measure(qualityGateStatus, c -> c.setValue(null).setData(
      "ERROR")));
    ComponentDto project3 = insertProject(c -> c.setName("Sonar Markdown"), new Measure(qualityGateStatus, c -> c.setValue(null).setData(
      "OK")));
    ComponentDto project4 = insertProject(c -> c.setName("Sonar Qube"), new Measure(qualityGateStatus,
      c -> c.setValue(null).setData("OK")));
    index();

    assertThat(call(request.setSort(QUALITY_GATE_STATUS).setAsc(true)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project3.getKey(), project4.getKey(), project2.getKey(), project1.getKey());
    assertThat(call(request.setSort(QUALITY_GATE_STATUS).setAsc(false)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project2.getKey(), project1.getKey(), project3.getKey(), project4.getKey());
  }

  @Test
  public void sort_by_last_analysis_date() {
    userSession.logIn();
    ProjectData project1 = db.components().insertPublicProject(p -> p.setKey("project1"));
    authorizationIndexerTester.allowOnlyAnyone(project1.getProjectDto());
    ProjectDto project2 = db.components().insertPublicProject(p -> p.setKey("project2")).getProjectDto();
    db.components().insertSnapshot(project2, snapshot -> snapshot.setCreatedAt(40_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project2);
    ProjectDto project3 = db.components().insertPublicProject(p -> p.setKey("project3")).getProjectDto();
    db.components().insertSnapshot(project3, snapshot -> snapshot.setCreatedAt(20_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project3);
    ProjectDto project4 = db.components().insertPublicProject(p -> p.setKey("project4")).getProjectDto();
    db.components().insertSnapshot(project4, snapshot -> snapshot.setCreatedAt(10_000_000_000L).setLast(false));
    db.components().insertSnapshot(project4, snapshot -> snapshot.setCreatedAt(30_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project4);
    index();

    List<Component> response = call(request.setSort(ANALYSIS_DATE).setAsc(true)).getComponentsList();
    assertThat(response).extracting(Component::getKey)
      .containsExactly(project3.getKey(), project4.getKey(), project2.getKey(), project1.getProjectDto().getKey());

    assertThat(call(request.setSort(ANALYSIS_DATE).setAsc(false)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project2.getKey(), project4.getKey(), project3.getKey(), project1.getProjectDto().getKey());
  }

  @Test
  public void return_last_analysis_date() {
    userSession.logIn();
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch1, snapshot -> snapshot.setCreatedAt(10_000_000_000L).setLast(false));
    db.components().insertSnapshot(mainBranch1, snapshot -> snapshot.setCreatedAt(20_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(projectData1.getProjectDto());
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto mainBranch2 = projectData2.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch2, snapshot -> snapshot.setCreatedAt(30_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(projectData2.getProjectDto());
    // No snapshot on project 3
    ProjectData projectData3 = db.components().insertPublicProject();
    ComponentDto mainBranch3 = projectData3.getMainBranchComponent();
    authorizationIndexerTester.allowOnlyAnyone(projectData3.getProjectDto());
    index();

    SearchProjectsWsResponse result = call(request.setAdditionalFields(singletonList("analysisDate")));

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::hasAnalysisDate, Component::getAnalysisDate)
      .containsOnly(
        tuple(mainBranch1.getKey(), true, formatDateTime(new Date(20_000_000_000L))),
        tuple(mainBranch2.getKey(), true, formatDateTime(new Date(30_000_000_000L))),
        tuple(mainBranch3.getKey(), false, ""));
  }

  @Ignore
  @Test
  public void return_leak_period_date() {
    when(editionProviderMock.get()).thenReturn(Optional.of(Edition.ENTERPRISE));
    userSession.logIn();
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    db.components().insertSnapshot(project1, snapshot -> snapshot.setPeriodDate(10_000_000_000L));
    authorizationIndexerTester.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(project1));
    // No leak period
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    db.components().insertSnapshot(project2, snapshot -> snapshot.setPeriodDate(null));
    authorizationIndexerTester.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(project2));
    // No snapshot on project 3
    ComponentDto project3 = db.components().insertPublicProject().getMainBranchComponent();
    authorizationIndexerTester.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(project3));

    MetricDto leakProjects = db.measures().insertMetric(c -> c.setKey(LEAK_PROJECTS_KEY).setValueType(DATA.name()));
    ComponentDto application1 = insertApplication(
      new Measure(leakProjects, c -> c.setData("{\"leakProjects\":[{\"id\": 1, \"leak\":20000000000}, {\"id\": 2, " +
        "\"leak\":10000000000}]}")));
    db.components().insertSnapshot(application1);

    authorizationIndexerTester.allowOnlyAnyone(db.components().getProjectDtoByMainBranch(application1));
    index();

    SearchProjectsWsResponse result = call(request.setAdditionalFields(singletonList("leakPeriodDate")));

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::hasLeakPeriodDate, Component::getLeakPeriodDate)
      .containsOnly(
        tuple(project1.getKey(), true, formatDateTime(new Date(10_000_000_000L))),
        tuple(project2.getKey(), false, ""),
        tuple(project3.getKey(), false, ""),
        tuple(application1.getKey(), true, formatDateTime(new Date(10_000_000_000L))));
  }

  @Test
  public void return_visibility_flag() {
    userSession.logIn();
    ProjectDto privateProject = db.components().insertPrivateProject(componentDto -> componentDto.setName("proj_A")).getProjectDto();
    authorizationIndexerTester.allowOnlyAnyone(privateProject);
    ProjectDto publicProject = db.components().insertPublicProject(componentDto -> componentDto.setName("proj_B")).getProjectDto();
    authorizationIndexerTester.allowOnlyAnyone(publicProject);
    index();

    userSession.addProjectPermission(UserRole.USER, privateProject);

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getVisibility)
      .containsExactly(
        tuple(privateProject.getKey(), privateProject.isPrivate() ? "private" : "public"),
        tuple(publicProject.getKey(), publicProject.isPrivate() ? "private" : "public"));
  }

  @Test
  @DataProvider({"true", "false"})
  public void return_ai_code_assured(Boolean aiCodeAssured) {
    when(aiCodeAssuranceVerifier.isAiCodeAssured(any())).thenReturn(aiCodeAssured);
    userSession.logIn();
    ProjectDto project = db.components().insertPublicProject(componentDto -> componentDto.setName("proj_A"),
      projectDto -> projectDto.setAiCodeAssurance(true)).getProjectDto();
    authorizationIndexerTester.allowOnlyAnyone(project);
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getIsAiCodeAssured)
      .containsExactly(
        tuple(project.getKey(), aiCodeAssured));
  }

  @Test
  public void does_not_return_branches() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    authorizationIndexerTester.allowOnlyAnyone(project);
    db.components().insertProjectBranch(project);
    index();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(project.getKey());
  }

  @Test
  public void fail_when_filter_metrics_are_unknown() {
    userSession.logIn();

    request.setFilter("debt > 80");

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Following metrics are not supported: 'debt'");
  }

  @Test
  public void fail_when_sort_metrics_are_unknown() {
    userSession.logIn();

    request.setSort("debt");

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Value of parameter 's' (debt) must be one of: [");
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    userSession.logIn();
    request.setPageSize(501);
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private SearchProjectsWsResponse call(RequestBuilder requestBuilder) {
    SearchProjectsRequest wsRequest = requestBuilder.build();
    TestRequest httpRequest = ws.newRequest();
    ofNullable(wsRequest.getFilter()).ifPresent(filter -> httpRequest.setParam(PARAM_FILTER, filter));
    ofNullable(wsRequest.getSort()).ifPresent(sort -> httpRequest.setParam(SORT, sort));
    ofNullable(wsRequest.getAsc()).ifPresent(asc -> httpRequest.setParam(ASCENDING, Boolean.toString(asc)));
    httpRequest.setParam(PAGE, String.valueOf(wsRequest.getPage()));
    httpRequest.setParam(PAGE_SIZE, String.valueOf(wsRequest.getPageSize()));
    httpRequest.setParam(FACETS, Joiner.on(",").join(wsRequest.getFacets()));
    httpRequest.setParam(FIELDS, Joiner.on(",").join(wsRequest.getAdditionalFields()));
    return httpRequest.executeProtobuf(SearchProjectsWsResponse.class);
  }

  private void addFavourite(ProjectDto project) {
    addFavourite(project.getUuid(), project.getKey(), project.getName(), project.getQualifier());
  }

  private void addFavourite(@Nullable String entityUuid, @Nullable String entityKey, @Nullable String entityName,
    @Nullable String qualifier) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("favourite")
      .setEntityUuid(entityUuid).setUserUuid(userSession.getUuid()), userSession.getLogin(), entityKey, entityName, qualifier);
    dbSession.commit();
  }

  private int projectCount = 0;

  private ComponentDto insertProject(Measure... measures) {
    return insertProject(defaults(), projectDto -> projectDto.setName("project_" + projectCount++), measures);
  }

  private ComponentDto insertProject(Consumer<ComponentDto> componentConsumer, Measure... measures) {
    return insertProject(componentConsumer, defaults(), measures);
  }

  private ComponentDto insertProject(Consumer<ComponentDto> componentConsumer, Consumer<ProjectDto> projectConsumer, Measure... measures) {
    ComponentDto project = db.components().insertPublicProject(componentConsumer, projectConsumer).getMainBranchComponent();
    Arrays.stream(measures).forEach(m -> db.measures().insertLiveMeasure(project, m.metric, m.consumer));
    return project;
  }

  private int applicationCount = 0;

  private ComponentDto insertApplication(Measure... measures) {
    return insertApplication(componentDto -> componentDto.setName("app_" + applicationCount++), measures);
  }

  private ComponentDto insertApplication(Consumer<ComponentDto> componentConsumer, Measure... measures) {
    ComponentDto application = db.components().insertPublicApplication(componentConsumer).getMainBranchComponent();
    Arrays.stream(measures).forEach(m -> db.measures().insertLiveMeasure(application, m.metric, m.consumer));
    return application;
  }

  private void index() {
    projectMeasuresIndexer.indexAll();
    ResultHandler<EntityDto> rh = r -> {
      if (!r.getResultObject().getQualifier().equals(Qualifiers.SUBVIEW)) {
        authorizationIndexerTester.allowOnlyAnyone(r.getResultObject());
      }
    };
    db.getDbClient().entityDao().scrollForIndexing(dbSession, rh);
  }

  private ComponentDto insertPortfolio() {
    return db.components().insertPublicPortfolio();
  }

  private static class Measure {
    private final MetricDto metric;
    private final Consumer<LiveMeasureDto> consumer;

    public Measure(MetricDto metric, Consumer<LiveMeasureDto> consumer) {
      this.metric = metric;
      this.consumer = consumer;
    }
  }

  private static <T> Consumer<T> defaults() {
    return t -> {
    };
  }
}
