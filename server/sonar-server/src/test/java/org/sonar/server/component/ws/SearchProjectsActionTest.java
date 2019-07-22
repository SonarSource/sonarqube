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
package org.sonar.server.component.ws;

import com.google.common.base.Joiner;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.component.ws.SearchProjectsAction.RequestBuilder;
import org.sonar.server.component.ws.SearchProjectsAction.SearchProjectsRequest;
import org.sonar.server.es.EsTester;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.qualitygate.ProjectsInWarning;
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
import static org.assertj.core.api.Assertions.tuple;
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
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_003;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

@RunWith(DataProviderRunner.class)
public class SearchProjectsActionTest {

  private static final String NCLOC = "ncloc";
  private static final String COVERAGE = "coverage";
  private static final String NEW_COVERAGE = "new_coverage";
  private static final String QUALITY_GATE_STATUS = "alert_status";
  private static final String ANALYSIS_DATE = "analysisDate";
  private static final String IS_FAVOURITE_CRITERION = "isFavorite";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @DataProvider
  public static Object[][] rating_metric_keys() {
    return new Object[][] {{SQALE_RATING_KEY}, {RELIABILITY_RATING_KEY}, {SECURITY_RATING_KEY}};
  }

  @DataProvider
  public static Object[][] new_rating_metric_keys() {
    return new Object[][] {{NEW_MAINTAINABILITY_RATING_KEY}, {NEW_RELIABILITY_RATING_KEY}, {NEW_SECURITY_RATING_KEY}};
  }

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, new ProjectMeasuresIndexer(dbClient, es.client()));
  private ProjectMeasuresIndex index = new ProjectMeasuresIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);
  private ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(db.getDbClient(), es.client());
  private ProjectsInWarning projectsInWarning = new ProjectsInWarning();

  private WsActionTester ws = new WsActionTester(new SearchProjectsAction(dbClient, index, userSession, projectsInWarning));

  private RequestBuilder request = SearchProjectsRequest.builder();

  @Before
  public void setUp() throws Exception {
    projectsInWarning.update(0L);
  }

  @Test
  public void verify_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.key()).isEqualTo("search_projects");
    assertThat(def.since()).isEqualTo("6.2");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params().stream().map(Param::key).collect(toList())).containsOnly("organization", "filter", "facets", "s", "asc", "ps", "p", "f");
    assertThat(def.changelog()).hasSize(1);

    Param organization = def.param("organization");
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.description()).isEqualTo("the organization to search projects in");
    assertThat(organization.since()).isEqualTo("6.3");

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
      "new_reliability_rating",
      "new_coverage",
      "new_security_rating",
      "sqale_rating",
      "new_duplicated_lines_density",
      "alert_status",
      "ncloc",
      "new_maintainability_rating",
      "name",
      "analysisDate");

    Param asc = def.param("asc");
    assertThat(asc.defaultValue()).isEqualTo("true");
    assertThat(asc.possibleValues()).containsOnly("true", "false", "yes", "no");

    Param f = def.param("f");
    assertThat(f.defaultValue()).isNull();
    assertThat(f.possibleValues()).containsOnly("_all", "organizations", "analysisDate", "leakPeriodDate");

    Param facets = def.param("facets");
    assertThat(facets.defaultValue()).isNull();
    assertThat(facets.possibleValues()).containsOnly("ncloc", "duplicated_lines_density", "coverage", "sqale_rating", "reliability_rating", "security_rating", "alert_status",
      "languages", "tags", "new_reliability_rating", "new_security_rating", "new_maintainability_rating", "new_coverage", "new_duplicated_lines_density", "new_lines");
  }

  @Test
  public void json_example() {
    userSession.logIn();
    OrganizationDto organization1Dto = db.organizations().insert(dto -> dto.setKey("my-org-key-1").setName("Foo"));
    OrganizationDto organization2Dto = db.organizations().insert(dto -> dto.setKey("my-org-key-2").setName("Bar"));

    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(organization1Dto, c -> c
      .setDbKey(KEY_PROJECT_EXAMPLE_001)
      .setName("My Project 1")
      .setTagsString("finance, java"),
      new Measure(coverage, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(organization1Dto, c -> c
      .setDbKey(KEY_PROJECT_EXAMPLE_002)
      .setName("My Project 2"),
      new Measure(coverage, c -> c.setValue(90d)));
    ComponentDto project3 = insertProject(organization2Dto, c -> c
      .setDbKey(KEY_PROJECT_EXAMPLE_003)
      .setName("My Project 3")
      .setTagsString("sales, offshore, java"),
      new Measure(coverage, c -> c.setValue(20d)));
    addFavourite(project1);

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
      .containsExactly(project1.getDbKey(), project2.getDbKey(), project3.getDbKey());
  }

  @Test
  public void order_by_name_case_insensitive() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    insertProject(organization, c -> c.setName("Maven"));
    insertProject(organization, c -> c.setName("Apache"));
    insertProject(organization, c -> c.setName("guava"));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName)
      .containsExactly("Apache", "guava", "Maven");
  }

  @Test
  public void paginate_result() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    IntStream.rangeClosed(1, 9).forEach(i -> insertProject(organization, c -> c.setName("PROJECT-" + i)));

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

    assertThat(result.getComponentsCount()).isEqualTo(0);
    Common.Paging paging = result.getPaging();
    assertThat(paging.getPageIndex()).isEqualTo(1);
    assertThat(paging.getPageSize()).isEqualTo(100);
    assertThat(paging.getTotal()).isEqualTo(0);
  }

  @Test
  public void filter_projects_with_query() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType(INT.name()));
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organizationDto,
      new Measure(coverage, c -> c.setValue(81d)),
      new Measure(ncloc, c -> c.setValue(10_000d)));
    ComponentDto project2 = insertProject(organizationDto,
      new Measure(coverage, c -> c.setValue(80d)),
      new Measure(ncloc, c -> c.setValue(10_000d)));
    ComponentDto project3 = insertProject(organizationDto,
      new Measure(coverage, c -> c.setValue(80d)),
      new Measure(ncloc, c -> c.setValue(10_001d)));

    SearchProjectsWsResponse result = call(request.setFilter("coverage <= 80 and ncloc <= 10000"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(project2.getDbKey());
  }

  @Test
  public void filter_projects_with_query_within_specified_organization() {
    userSession.logIn();
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organization1, new Measure(coverage, c -> c.setValue(81d)), new Measure(ncloc, c -> c.setValue(10_000d)));
    ComponentDto project2 = insertProject(organization1, new Measure(coverage, c -> c.setValue(80d)), new Measure(ncloc, c -> c.setValue(10_000d)));
    ComponentDto project3 = insertProject(organization2, new Measure(coverage, c -> c.setValue(80d)), new Measure(ncloc, c -> c.setValue(10_000d)));

    assertThat(call(request.setOrganization(null)).getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.getDbKey(), project2.getDbKey(), project3.getDbKey());
    assertThat(call(request.setOrganization(organization1.getKey())).getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project1.getDbKey(), project2.getDbKey());
    assertThat(call(request.setOrganization(organization2.getKey())).getComponentsList())
      .extracting(Component::getKey)
      .containsOnly(project3.getDbKey());
  }

  @Test
  public void filter_projects_by_quality_gate() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(QUALITY_GATE_STATUS).setValueType(LEVEL.name()));
    ComponentDto project1 = insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));
    ComponentDto project2 = insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));
    ComponentDto project3 = insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setValue(null).setData("ERROR")));

    SearchProjectsWsResponse result = call(request.setFilter("alert_status = OK"));

    assertThat(result.getComponentsList())
      .extracting(Component::getKey)
      .containsExactlyInAnyOrder(project1.getDbKey(), project2.getDbKey());
  }

  @Test
  public void filter_projects_by_languages() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto languagesDistribution = db.measures().insertMetric(c -> c.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY).setValueType("DATA"));
    ComponentDto project1 = insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("<null>=2;java=6;xoo=18")));
    ComponentDto project2 = insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("java=3;xoo=9")));
    ComponentDto project3 = insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("xoo=1")));
    ComponentDto project4 = insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("<null>=1;java=5;xoo=13")));

    SearchProjectsWsResponse result = call(request.setFilter("languages IN (java, js, <null>)"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project2.getDbKey(), project4.getDbKey());
  }

  @Test
  @UseDataProvider("rating_metric_keys")
  public void filter_projects_by_rating(String metricKey) {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(metricKey).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organizationDto, new Measure(ratingMetric, c -> c.setValue(1d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(ratingMetric, c -> c.setValue(2d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(ratingMetric, c -> c.setValue(3d)));

    SearchProjectsWsResponse result = call(request.setFilter(metricKey + " = 2"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(project2.getDbKey());
  }

  @Test
  @UseDataProvider("new_rating_metric_keys")
  public void filter_projects_by_new_rating(String newMetricKey) {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(newMetricKey).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organizationDto, new Measure(ratingMetric, c -> c.setVariation(1d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(ratingMetric, c -> c.setVariation(2d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(ratingMetric, c -> c.setVariation(3d)));

    SearchProjectsWsResponse result = call(request.setFilter(newMetricKey + " = 2"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(project2.getDbKey());
  }

  @Test
  public void filter_projects_by_tags() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project1 = insertProject(organizationDto, c -> c.setTags(asList("finance", "platform")));
    ComponentDto project2 = insertProject(organizationDto, c -> c.setTags(singletonList("marketing")));
    ComponentDto project3 = insertProject(organizationDto, c -> c.setTags(singletonList("offshore")));

    SearchProjectsWsResponse result = call(request.setFilter("tags in (finance, offshore)"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_coverage() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(organizationDto, new Measure(coverage, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(coverage, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(coverage, c -> c.setValue(10d)));

    SearchProjectsWsResponse result = call(request.setFilter("coverage <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_new_coverage() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_COVERAGE).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(80d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(85d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(10d)));

    SearchProjectsWsResponse result = call(request.setFilter("new_coverage <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_duplications() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto duplications = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(organizationDto, new Measure(duplications, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(duplications, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(duplications, c -> c.setValue(10d)));

    SearchProjectsWsResponse result = call(request.setFilter("duplicated_lines_density <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_no_duplication() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    MetricDto duplications = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(organizationDto, new Measure(coverage, c -> c.setValue(10d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(duplications, c -> c.setValue(0d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(duplications, c -> c.setValue(79d)));

    SearchProjectsWsResponse result = call(request.setFilter("duplicated_lines_density = NO_DATA"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey());
  }

  @Test
  public void filter_projects_by_no_duplication_should_not_return_projects_with_duplication() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    MetricDto duplications = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    insertProject(organizationDto, new Measure(duplications, c -> c.setValue(10d)), new Measure(coverage, c -> c.setValue(50d)));

    SearchProjectsWsResponse result = call(request.setFilter("duplicated_lines_density = NO_DATA"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).isEmpty();
  }

  @Test
  public void filter_projects_by_new_duplications() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto newDuplications = db.measures().insertMetric(c -> c.setKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    ComponentDto project1 = insertProject(organizationDto, new Measure(newDuplications, c -> c.setVariation(80d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(newDuplications, c -> c.setVariation(85d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(newDuplications, c -> c.setVariation(10d)));

    SearchProjectsWsResponse result = call(request.setFilter("new_duplicated_lines_density <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_ncloc() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(80d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(85d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(10d)));

    SearchProjectsWsResponse result = call(request.setFilter("ncloc <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_new_lines() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto newLines = db.measures().insertMetric(c -> c.setKey(NEW_LINES_KEY).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organizationDto, new Measure(newLines, c -> c.setVariation(80d)));
    ComponentDto project2 = insertProject(organizationDto, new Measure(newLines, c -> c.setVariation(85d)));
    ComponentDto project3 = insertProject(organizationDto, new Measure(newLines, c -> c.setVariation(10d)));

    SearchProjectsWsResponse result = call(request.setFilter("new_lines <= 80"));

    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactlyInAnyOrder(project1.getDbKey(), project3.getDbKey());
  }

  @Test
  public void filter_projects_by_text_query() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    insertProject(organizationDto, c -> c.setDbKey("sonar-java").setName("Sonar Java"));
    insertProject(organizationDto, c -> c.setDbKey("sonar-groovy").setName("Sonar Groovy"));
    insertProject(organizationDto, c -> c.setDbKey("sonar-markdown").setName("Sonar Markdown"));
    insertProject(organizationDto, c -> c.setDbKey("sonarqube").setName("Sonar Qube"));

    assertThat(call(request.setFilter("query = \"Groovy\"")).getComponentsList()).extracting(Component::getName).containsOnly("Sonar Groovy");
    assertThat(call(request.setFilter("query = \"oNar\"")).getComponentsList()).extracting(Component::getName).containsOnly("Sonar Java", "Sonar Groovy", "Sonar Markdown",
      "Sonar Qube");
    assertThat(call(request.setFilter("query = \"sonar-java\"")).getComponentsList()).extracting(Component::getName).containsOnly("Sonar Java");
  }

  @Test
  public void filter_favourite_projects_with_query_with_or_without_a_specified_organization() {
    userSession.logIn();
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();
    OrganizationDto organization4 = db.organizations().insert();
    OrganizationDto organization5 = db.organizations().insert();
    ComponentDto favourite1_1 = insertProject(organization1);
    ComponentDto favourite1_2 = insertProject(organization1);
    ComponentDto nonFavourite1 = insertProject(organization1);
    ComponentDto favourite2 = insertProject(organization2);
    ComponentDto nonFavourite2 = insertProject(organization2);
    ComponentDto favourite3 = insertProject(organization3);
    ComponentDto nonFavourite4 = insertProject(organization4);
    Stream.of(favourite1_1, favourite1_2, favourite2, favourite3).forEach(this::addFavourite);

    assertThat(call(request.setFilter(null).setOrganization(null)).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(favourite1_1.name(), favourite1_2.name(), nonFavourite1.name(), favourite2.name(), nonFavourite2.name(), favourite3.name(), nonFavourite4.name());
    assertThat(call(request.setFilter(IS_FAVOURITE_CRITERION).setOrganization(null)).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(favourite1_1.name(), favourite1_2.name(), favourite2.name(), favourite3.name());
    assertThat(call(request.setFilter(null).setOrganization(organization1.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(favourite1_1.name(), favourite1_2.name(), nonFavourite1.name());
    assertThat(call(request.setFilter(IS_FAVOURITE_CRITERION).setOrganization(organization1.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(favourite1_1.name(), favourite1_2.name());
    assertThat(call(request.setFilter(null).setOrganization(organization3.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(favourite3.name());
    assertThat(call(request.setFilter(IS_FAVOURITE_CRITERION).setOrganization(organization3.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(favourite3.name());
    assertThat(call(request.setFilter(null).setOrganization(organization4.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly(nonFavourite4.name());
    assertThat(call(request.setFilter(IS_FAVOURITE_CRITERION).setOrganization(organization4.getKey())).getComponentsList())
      .isEmpty();
    assertThat(call(request.setFilter(null).setOrganization(organization5.getKey())).getComponentsList())
      .isEmpty();
    assertThat(call(request.setFilter(IS_FAVOURITE_CRITERION).setOrganization(organization5.getKey())).getComponentsList())
      .isEmpty();
  }

  @Test
  public void filter_projects_on_favorites() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto javaProject = insertProject(organization);
    ComponentDto markDownProject = insertProject(organization);
    ComponentDto sonarQubeProject = insertProject(organization);
    Stream.of(javaProject, markDownProject).forEach(this::addFavourite);

    SearchProjectsWsResponse result = call(request.setFilter("isFavorite"));

    assertThat(result.getComponentsCount()).isEqualTo(2);
    assertThat(result.getComponentsList()).extracting(Component::getKey).containsExactly(javaProject.getDbKey(), markDownProject.getDbKey());
  }

  @Test
  public void filtering_on_favorites_returns_empty_results_if_not_logged_in() {
    userSession.anonymous();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto javaProject = insertProject(organization);
    ComponentDto markDownProject = insertProject(organization);
    ComponentDto sonarQubeProject = insertProject(organization);
    Stream.of(javaProject, markDownProject).forEach(this::addFavourite);

    SearchProjectsWsResponse result = call(request.setFilter("isFavorite"));

    assertThat(result.getComponentsCount()).isZero();
  }

  @Test
  public void do_not_return_isFavorite_if_anonymous_user() {
    userSession.anonymous();
    OrganizationDto organization = db.organizations().insert();
    insertProject(organization);

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::hasIsFavorite).containsExactlyInAnyOrder(false);
  }

  @Test
  public void return_nloc_facet() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto ncloc = db.measures().insertMetric(c -> c.setKey(NCLOC).setValueType(INT.name()));
    insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(5d)));
    insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(5d)));
    insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(10_000d)));
    insertProject(organizationDto, new Measure(ncloc, c -> c.setValue(500_001d)));

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_LINES_KEY).setValueType(INT.name()));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(100d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(15_000d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(50_000d)));

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto languagesDistribution = db.measures().insertMetric(c -> c.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY).setValueType("DATA"));
    insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("<null>=2;java=6;xoo=18")));
    insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("java=5;xoo=19")));
    insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("xoo=1")));
    insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("<null>=1;java=3;xoo=8")));

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto languagesDistribution = db.measures().insertMetric(c -> c.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY).setValueType("DATA"));
    insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("<null>=2;java=6")));
    insertProject(organizationDto, new Measure(languagesDistribution, c -> c.setValue(null).setData("java=5")));

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
    OrganizationDto organization = db.getDefaultOrganization();
    insertProject(organization, c -> c.setTags(asList("finance", "platform")));
    insertProject(organization, c -> c.setTags(singletonList("offshore")));
    insertProject(organization, c -> c.setTags(singletonList("offshore")));

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
    OrganizationDto organization = db.getDefaultOrganization();
    insertProject(organization, c -> c.setTags(asList("finance", "platform")));
    insertProject(organization, c -> c.setTags(singletonList("offshore")));
    insertProject(organization, c -> c.setTags(singletonList("offshore")));

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
  @UseDataProvider("rating_metric_keys")
  public void return_rating_facet(String ratingMetricKey) {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    MetricDto ratingMetric = db.measures().insertMetric(c -> c.setKey(ratingMetricKey).setValueType("RATING"));
    insertProject(organization, new Measure(ratingMetric, c -> c.setValue(1d)));
    insertProject(organization, new Measure(ratingMetric, c -> c.setValue(1d)));
    insertProject(organization, new Measure(ratingMetric, c -> c.setValue(3d)));
    insertProject(organization, new Measure(ratingMetric, c -> c.setValue(5d)));

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
  @UseDataProvider("new_rating_metric_keys")
  public void return_new_rating_facet(String newRatingMetricKey) {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    MetricDto newRatingMetric = db.measures().insertMetric(c -> c.setKey(newRatingMetricKey).setValueType("RATING"));
    insertProject(organization, new Measure(newRatingMetric, c -> c.setVariation(1d)));
    insertProject(organization, new Measure(newRatingMetric, c -> c.setVariation(1d)));
    insertProject(organization, new Measure(newRatingMetric, c -> c.setVariation(3d)));
    insertProject(organization, new Measure(newRatingMetric, c -> c.setVariation(5d)));

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
  public void return_coverage_facet() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType("PERCENT"));
    insertProject(organizationDto);
    insertProject(organizationDto, new Measure(coverage, c -> c.setValue(80d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setValue(85d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setValue(10d)));

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_COVERAGE).setValueType("PERCENT"));
    insertProject(organizationDto);
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(80d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(85d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(10d)));

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    insertProject(organizationDto, new Measure(coverage, c -> c.setValue(10d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setValue(15d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setValue(5d)));
    insertProject(organizationDto);

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setValueType("PERCENT"));
    insertProject(organizationDto);
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(10d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(15d)));
    insertProject(organizationDto, new Measure(coverage, c -> c.setVariation(5d)));

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
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(ALERT_STATUS_KEY).setValueType(LEVEL.name()));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.WARN.name()).setValue(null)));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.OK.name()).setValue(null)));
    projectsInWarning.update(1L);

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(ALERT_STATUS_KEY)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> ALERT_STATUS_KEY.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(
        tuple("OK", 1L),
        tuple("ERROR", 2L),
        tuple("WARN", 1L));
  }

  @Test
  public void return_quality_gate_facet_without_warning_when_no_projects_in_warning() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(ALERT_STATUS_KEY).setValueType(LEVEL.name()));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.ERROR.name()).setValue(null)));
    insertProject(organizationDto, new Measure(qualityGateStatus, c -> c.setData(Metric.Level.OK.name()).setValue(null)));
    projectsInWarning.update(0L);

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
    OrganizationDto organization = db.getDefaultOrganization();
    insertProject(organization, c -> c.setName("Sonar Java"));
    insertProject(organization, c -> c.setName("Sonar Groovy"));
    insertProject(organization, c -> c.setName("Sonar Markdown"));
    insertProject(organization, c -> c.setName("Sonar Qube"));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName).containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown", "Sonar Qube");
  }

  @Test
  public void sort_by_name() {
    userSession.logIn();
    OrganizationDto organization = db.getDefaultOrganization();
    insertProject(organization, c -> c.setName("Sonar Java"));
    insertProject(organization, c -> c.setName("Sonar Groovy"));
    insertProject(organization, c -> c.setName("Sonar Markdown"));
    insertProject(organization, c -> c.setName("Sonar Qube"));

    assertThat(call(request.setSort("name").setAsc(true)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown", "Sonar Qube");
    assertThat(call(request.setSort("name").setAsc(false)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Qube", "Sonar Markdown", "Sonar Java", "Sonar Groovy");
  }

  @Test
  public void sort_by_coverage_then_by_name() {
    userSession.logIn();
    OrganizationDto organizationDto = db.organizations().insert();
    MetricDto coverage = db.measures().insertMetric(c -> c.setKey(COVERAGE).setValueType(INT.name()));
    ComponentDto project1 = insertProject(organizationDto, c -> c.setName("Sonar Java"), new Measure(coverage, c -> c.setValue(81d)));
    ComponentDto project2 = insertProject(organizationDto, c -> c.setName("Sonar Groovy"), new Measure(coverage, c -> c.setValue(81d)));
    ComponentDto project3 = insertProject(organizationDto, c -> c.setName("Sonar Markdown"), new Measure(coverage, c -> c.setValue(80d)));
    ComponentDto project4 = insertProject(organizationDto, c -> c.setName("Sonar Qube"), new Measure(coverage, c -> c.setValue(80d)));

    assertThat(call(request.setSort(COVERAGE).setAsc(true)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project3.getDbKey(), project4.getDbKey(), project2.getDbKey(), project1.getDbKey());
    assertThat(call(request.setSort(COVERAGE).setAsc(false)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project2.getDbKey(), project1.getDbKey(), project3.getDbKey(), project4.getDbKey());
  }

  @Test
  public void sort_by_quality_gate_then_by_name() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(QUALITY_GATE_STATUS).setValueType(LEVEL.name()));
    ComponentDto project1 = insertProject(organization, c -> c.setName("Sonar Java"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("ERROR")));
    ComponentDto project2 = insertProject(organization, c -> c.setName("Sonar Groovy"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("ERROR")));
    ComponentDto project3 = insertProject(organization, c -> c.setName("Sonar Markdown"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));
    ComponentDto project4 = insertProject(organization, c -> c.setName("Sonar Qube"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));

    assertThat(call(request.setSort(QUALITY_GATE_STATUS).setAsc(true)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project3.getDbKey(), project4.getDbKey(), project2.getDbKey(), project1.getDbKey());
    assertThat(call(request.setSort(QUALITY_GATE_STATUS).setAsc(false)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project2.getDbKey(), project1.getDbKey(), project3.getDbKey(), project4.getDbKey());
  }

  @Test
  public void sort_by_last_analysis_date() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization, p -> p.setDbKey("project1"));
    authorizationIndexerTester.allowOnlyAnyone(project1);
    ComponentDto project2 = db.components().insertPublicProject(organization, p -> p.setDbKey("project2"));
    db.components().insertSnapshot(project2, snapshot -> snapshot.setCreatedAt(40_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project2);
    ComponentDto project3 = db.components().insertPublicProject(organization, p -> p.setDbKey("project3"));
    db.components().insertSnapshot(project3, snapshot -> snapshot.setCreatedAt(20_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project3);
    ComponentDto project4 = db.components().insertPublicProject(organization, p -> p.setDbKey("project4"));
    db.components().insertSnapshot(project4, snapshot -> snapshot.setCreatedAt(10_000_000_000L).setLast(false));
    db.components().insertSnapshot(project4, snapshot -> snapshot.setCreatedAt(30_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project4);
    projectMeasuresIndexer.indexOnStartup(null);

    assertThat(call(request.setSort(ANALYSIS_DATE).setAsc(true)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project3.getDbKey(), project4.getDbKey(), project2.getDbKey(), project1.getDbKey());

    assertThat(call(request.setSort(ANALYSIS_DATE).setAsc(false)).getComponentsList()).extracting(Component::getKey)
      .containsExactly(project2.getDbKey(), project4.getDbKey(), project3.getDbKey(), project1.getDbKey());
  }

  @Test
  public void return_last_analysis_date() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization);
    db.components().insertSnapshot(project1, snapshot -> snapshot.setCreatedAt(10_000_000_000L).setLast(false));
    db.components().insertSnapshot(project1, snapshot -> snapshot.setCreatedAt(20_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project1);
    ComponentDto project2 = db.components().insertPublicProject(organization);
    db.components().insertSnapshot(project2, snapshot -> snapshot.setCreatedAt(30_000_000_000L).setLast(true));
    authorizationIndexerTester.allowOnlyAnyone(project2);
    // No snapshot on project 3
    ComponentDto project3 = db.components().insertPublicProject(organization);
    authorizationIndexerTester.allowOnlyAnyone(project3);
    projectMeasuresIndexer.indexOnStartup(null);

    SearchProjectsWsResponse result = call(request.setAdditionalFields(singletonList("analysisDate")));

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::hasAnalysisDate, Component::getAnalysisDate)
      .containsOnly(
        tuple(project1.getDbKey(), true, formatDateTime(new Date(20_000_000_000L))),
        tuple(project2.getDbKey(), true, formatDateTime(new Date(30_000_000_000L))),
        tuple(project3.getDbKey(), false, ""));
  }

  @Test
  public void return_leak_period_date() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization);
    db.components().insertSnapshot(project1, snapshot -> snapshot.setPeriodDate(10_000_000_000L));
    authorizationIndexerTester.allowOnlyAnyone(project1);
    // No leak period
    ComponentDto project2 = db.components().insertPublicProject(organization);
    db.components().insertSnapshot(project2, snapshot -> snapshot.setPeriodDate(null));
    authorizationIndexerTester.allowOnlyAnyone(project2);
    // No snapshot on project 3
    ComponentDto project3 = db.components().insertPublicProject(organization);
    authorizationIndexerTester.allowOnlyAnyone(project3);
    projectMeasuresIndexer.indexOnStartup(null);

    SearchProjectsWsResponse result = call(request.setAdditionalFields(singletonList("leakPeriodDate")));

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::hasLeakPeriodDate, Component::getLeakPeriodDate)
      .containsOnly(
        tuple(project1.getDbKey(), true, formatDateTime(new Date(10_000_000_000L))),
        tuple(project2.getDbKey(), false, ""),
        tuple(project3.getDbKey(), false, ""));
  }

  @Test
  public void return_visibility_flag() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto privateProject = db.components().insertPublicProject(organization);
    authorizationIndexerTester.allowOnlyAnyone(privateProject);
    ComponentDto publicProject = db.components().insertPrivateProject(organization);
    authorizationIndexerTester.allowOnlyAnyone(publicProject);
    projectMeasuresIndexer.indexOnStartup(null);

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getVisibility)
      .containsExactly(
        tuple(privateProject.getDbKey(), privateProject.isPrivate() ? "private" : "public"),
        tuple(publicProject.getDbKey(), publicProject.isPrivate() ? "private" : "public"));
  }

  @Test
  public void does_not_return_branches() {
    ComponentDto project = db.components().insertMainBranch();
    authorizationIndexerTester.allowOnlyAnyone(project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    projectMeasuresIndexer.indexOnStartup(null);

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(project.getDbKey());
  }

  @Test
  public void use_deprecated_warning_quality_gate_in_filter() {
    userSession.logIn();
    OrganizationDto organization = db.organizations().insert();
    MetricDto qualityGateStatus = db.measures().insertMetric(c -> c.setKey(QUALITY_GATE_STATUS).setValueType(LEVEL.name()));
    ComponentDto project1 = insertProject(organization, c -> c.setName("Sonar Java"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("ERROR")));
    ComponentDto project2 = insertProject(organization, c -> c.setName("Sonar Groovy"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("WARN")));
    ComponentDto project3 = insertProject(organization, c -> c.setName("Sonar Markdown"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("WARN")));
    ComponentDto project4 = insertProject(organization, c -> c.setName("Sonar Qube"), new Measure(qualityGateStatus, c -> c.setValue(null).setData("OK")));

    List<Component> projects = call(request
      .setFilter("alert_status = WARN"))
        .getComponentsList();

    assertThat(projects)
      .extracting(Component::getKey)
      .containsExactly(project2.getKey(), project3.getKey());
  }

  @Test
  public void fail_when_filter_metrics_are_unknown() {
    userSession.logIn();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following metrics are not supported: 'debt'");

    request.setFilter("debt > 80");

    call(request);
  }

  @Test
  public void fail_when_sort_metrics_are_unknown() {
    userSession.logIn();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 's' (debt) must be one of: [");

    request.setSort("debt");

    call(request);
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);

    call(request.setPageSize(501));
  }

  private SearchProjectsWsResponse call(RequestBuilder requestBuilder) {
    SearchProjectsRequest wsRequest = requestBuilder.build();
    TestRequest httpRequest = ws.newRequest();
    ofNullable(wsRequest.getOrganization()).ifPresent(organization -> httpRequest.setParam(PARAM_ORGANIZATION, organization));
    ofNullable(wsRequest.getFilter()).ifPresent(filter -> httpRequest.setParam(PARAM_FILTER, filter));
    ofNullable(wsRequest.getSort()).ifPresent(sort -> httpRequest.setParam(SORT, sort));
    ofNullable(wsRequest.getAsc()).ifPresent(asc -> httpRequest.setParam(ASCENDING, Boolean.toString(asc)));
    httpRequest.setParam(PAGE, String.valueOf(wsRequest.getPage()));
    httpRequest.setParam(PAGE_SIZE, String.valueOf(wsRequest.getPageSize()));
    httpRequest.setParam(FACETS, Joiner.on(",").join(wsRequest.getFacets()));
    httpRequest.setParam(FIELDS, Joiner.on(",").join(wsRequest.getAdditionalFields()));
    return httpRequest.executeProtobuf(SearchProjectsWsResponse.class);
  }

  private void addFavourite(ComponentDto project) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("favourite").setResourceId(project.getId()).setUserId(userSession.getUserId()));
    dbSession.commit();
  }

  private ComponentDto insertProject(OrganizationDto organizationDto, Measure... measures) {
    return insertProject(organizationDto, c -> {
    }, measures);
  }

  private ComponentDto insertProject(OrganizationDto organizationDto, Consumer<ComponentDto> projectConsumer, Measure... measures) {
    ComponentDto project = db.components().insertPublicProject(organizationDto, projectConsumer);
    Arrays.stream(measures).forEach(m -> db.measures().insertLiveMeasure(project, m.metric, m.consumer));
    authorizationIndexerTester.allowOnlyAnyone(project);
    projectMeasuresIndexer.indexOnAnalysis(project.uuid());
    return project;
  }

  private static class Measure {
    private final MetricDto metric;
    private final Consumer<LiveMeasureDto> consumer;

    public Measure(MetricDto metric, Consumer<LiveMeasureDto> consumer) {
      this.metric = metric;
      this.consumer = consumer;
    }
  }
}
