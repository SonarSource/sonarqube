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
package org.sonar.server.component.ws;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.measure.index.ProjectMeasuresDoc;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.KeyExamples;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
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
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newPublicProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

public class SearchProjectsActionTest {

  private static final String NCLOC = "ncloc";
  private static final String COVERAGE = "coverage";
  private static final String QUALITY_GATE_STATUS = "alert_status";
  private static final String IS_FAVOURITE_CRITERION = "isFavorite";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setUserId(23);
  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, new ProjectMeasuresIndexer(dbClient, es.client()));
  private ProjectMeasuresIndex index = new ProjectMeasuresIndex(es.client(), new AuthorizationTypeSupport(userSession));
  private ProjectMeasuresQueryValidator queryValidator = new ProjectMeasuresQueryValidator(dbClient);

  private WsActionTester ws = new WsActionTester(
    new SearchProjectsAction(dbClient, index, queryValidator, userSession));

  private SearchProjectsRequest.Builder request = SearchProjectsRequest.builder();

  @Test
  public void verify_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.key()).isEqualTo("search_projects");
    assertThat(def.since()).isEqualTo("6.2");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params().stream().map(Param::key).collect(toList())).containsOnly("organization", "filter", "facets", "s", "asc", "ps", "p", "f");
    assertThat(def.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("6.4", "The 'languages' parameter accepts 'filter' to filter by language"),
      tuple("6.4", "The 'visibility' field is added"));

    Param organization = def.param("organization");
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.description()).isEqualTo("the organization to search projects in");
    assertThat(organization.since()).isEqualTo("6.3");

    Param sort = def.param("s");
    assertThat(sort.defaultValue()).isEqualTo("name");
    assertThat(sort.exampleValue()).isEqualTo("ncloc");
    assertThat(sort.possibleValues()).isNull();

    Param asc = def.param("asc");
    assertThat(asc.defaultValue()).isEqualTo("true");
    assertThat(asc.possibleValues()).containsOnly("true", "false", "yes", "no");

    Param additionalFields = def.param("f");
    assertThat(additionalFields.defaultValue()).isNull();
    assertThat(additionalFields.possibleValues()).containsOnly("analysisDate");

    Param facets = def.param("facets");
    assertThat(facets.defaultValue()).isNull();
    assertThat(facets.possibleValues()).containsOnly("ncloc", "duplicated_lines_density", "coverage", "sqale_rating", "reliability_rating", "security_rating", "alert_status",
      "languages", "tags");
  }

  @Test
  public void json_example() {
    OrganizationDto organization1Dto = db.organizations().insertForKey("my-org-key-1");
    OrganizationDto organization2Dto = db.organizations().insertForKey("my-org-key-2");
    ComponentDto project1 = insertProjectInDbAndEs(ComponentTesting.newPublicProjectDto(organization1Dto)
      .setUuid(Uuids.UUID_EXAMPLE_01)
      .setKey(KeyExamples.KEY_PROJECT_EXAMPLE_001)
      .setName("My Project 1")
      .setTagsString("finance, java"));
    insertProjectInDbAndEs(ComponentTesting.newPublicProjectDto(organization1Dto)
      .setUuid(Uuids.UUID_EXAMPLE_02)
      .setKey(KeyExamples.KEY_PROJECT_EXAMPLE_002)
      .setName("My Project 2"));
    insertProjectInDbAndEs(ComponentTesting.newPublicProjectDto(organization2Dto)
      .setUuid(Uuids.UUID_EXAMPLE_03)
      .setKey(KeyExamples.KEY_PROJECT_EXAMPLE_003)
      .setName("My Project 3")
      .setTagsString("sales, offshore, java"));
    userSession.logIn().setUserId(23);
    addFavourite(project1);
    dbSession.commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void order_by_name_case_insensitive() {
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("Maven"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("Apache"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("guava"));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName)
      .containsExactly("Apache", "guava", "Maven");
  }

  @Test
  public void paginate_result() {
    IntStream.rangeClosed(1, 9).forEach(i -> insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("PROJECT-" + i)));

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
    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(0);
    Common.Paging paging = result.getPaging();
    assertThat(paging.getPageIndex()).isEqualTo(1);
    assertThat(paging.getPageSize()).isEqualTo(100);
    assertThat(paging.getTotal()).isEqualTo(0);
  }

  @Test
  public void return_only_projects() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto).setName("SonarQube");
    ComponentDto directory = newDirectory(project, "path");
    insertProjectInDbAndEs(project);
    componentDb.insertComponents(newModuleDto(project), newView(organizationDto), directory, newFileDto(project, directory));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(1);
    assertThat(result.getComponents(0).getName()).isEqualTo("SonarQube");
  }

  @Test
  public void filter_projects_with_query() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-key-1");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_001d)));
    insertMetrics(COVERAGE, NCLOC);
    request.setFilter("coverage <= 80 and ncloc <= 10000");

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(1);
    assertThat(result.getComponents(0).getName()).isEqualTo("Sonar Markdown");
  }

  @Test
  public void filter_projects_with_query_within_specified_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization1).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization1).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization2).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_001d)));
    insertMetrics(COVERAGE, NCLOC);

    assertThat(call(request.setOrganization(null)).getComponentsList())
      .extracting(Component::getName)
      .containsOnly("Sonar Java", "Sonar Markdown", "Sonar Qube");
    assertThat(call(request.setOrganization(organization1.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly("Sonar Java", "Sonar Markdown");
    assertThat(call(request.setOrganization(organization2.getKey())).getComponentsList())
      .extracting(Component::getName)
      .containsOnly("Sonar Qube");
  }

  @Test
  public void filter_projects_by_quality_gate() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-key-1");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Java"), "OK");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Markdown"), "OK");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Qube"), "ERROR");
    insertMetrics(COVERAGE, NCLOC);
    request.setFilter("alert_status = OK");

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName).containsOnly("Sonar Java", "Sonar Markdown");
  }

  @Test
  public void filter_projects_by_languages() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-key-1");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81d)), null, asList("<null>", "java", "xoo"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81)), null, asList("java", "xoo"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d)), null, asList("xoo"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d)), null, asList("<null>", "java", "xoo"));
    insertMetrics(COVERAGE, NCLOC_LANGUAGE_DISTRIBUTION_KEY);
    request.setFilter("languages IN (java, js, <null>)");

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName).containsOnly("Sonar Java", "Sonar Groovy", "Sonar Qube");
  }

  @Test
  public void filter_projects_by_tags() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-key-1");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Java").setTags(newArrayList("finance", "platform")));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Markdown").setTags(singletonList("marketing")));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setName("Sonar Qube").setTags(newArrayList("offshore")));
    request.setFilter("tags in (finance, offshore)");

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName).containsOnly("Sonar Java", "Sonar Qube");
  }

  @Test
  public void filter_projects_by_text_query() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-key-1");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setKey("sonar-java").setName("Sonar Java"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setKey("sonar-groovy").setName("Sonar Groovy"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setKey("sonar-markdown").setName("Sonar Markdown"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto).setKey("sonarqube").setName("Sonar Qube"));

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
    List<Map<String, Object>> someMeasure = singletonList(newMeasure(COVERAGE, 81));
    ComponentDto favourite1_1 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization1), someMeasure);
    ComponentDto favourite1_2 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization1), someMeasure);
    ComponentDto nonFavourite1 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization1), someMeasure);
    ComponentDto favourite2 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization2), someMeasure);
    ComponentDto nonFavourite2 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization2), someMeasure);
    ComponentDto favourite3 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization3), someMeasure);
    ComponentDto nonFavourite4 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization4), someMeasure);
    Stream.of(favourite1_1, favourite1_2, favourite2, favourite3)
      .forEach(this::addFavourite);
    insertMetrics(COVERAGE, NCLOC);

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
    ComponentDto javaProject = insertProjectInDbAndEs(newPrivateProjectDto(db.getDefaultOrganization(), "java-id").setName("Sonar Java"),
      newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 10_000d)));
    ComponentDto markDownProject = insertProjectInDbAndEs(newPrivateProjectDto(db.getDefaultOrganization(), "markdown-id").setName("Sonar Markdown"),
      newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_001d)));
    addFavourite(javaProject);
    addFavourite(markDownProject);
    dbSession.commit();
    request.setFilter("isFavorite");

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(2);
    assertThat(result.getComponentsList()).extracting(Component::getId).containsExactly("java-id", "markdown-id");
  }

  @Test
  public void filtering_on_favorites_returns_empty_results_if_not_logged_in() {
    ComponentDto javaProject = insertProjectInDbAndEs(newPrivateProjectDto(db.getDefaultOrganization(), "java-id").setName("Sonar Java"),
      newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_001d)));
    addFavourite(javaProject);
    dbSession.commit();
    request.setFilter("isFavorite");
    userSession.anonymous();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(0);
  }

  @Test
  public void do_not_return_isFavorite_if_anonymous_user() {
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81)));
    insertMetrics(COVERAGE);
    userSession.anonymous();

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(1);
    assertThat(result.getComponents(0).hasIsFavorite()).isFalse();
  }

  @Test
  public void empty_list_if_isFavorite_filter_and_anonymous_user() {
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81)));
    insertMetrics(COVERAGE);
    userSession.anonymous();
    request.setFilter("isFavorite");

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(0);
  }

  @Test
  public void return_nloc_facet() {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 500_001d)));
    insertMetrics(COVERAGE, NCLOC);

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(NCLOC)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> NCLOC.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getProperty()).isEqualTo(NCLOC);
    assertThat(facet.getValuesCount()).isEqualTo(5);
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
  public void return_languages_facet() {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81d)), null, asList("<null>", "java", "xoo"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81)), null, asList("java", "xoo"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d)), null, asList("xoo"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d)), null, asList("<null>", "java", "xoo"));
    insertMetrics(COVERAGE, NCLOC_LANGUAGE_DISTRIBUTION_KEY);

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(FILTER_LANGUAGES)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_LANGUAGES.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getProperty()).isEqualTo(FILTER_LANGUAGES);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("xoo", 4L),
        tuple("java", 3L),
        tuple("<null>", 2L));
  }

  @Test
  public void return_languages_facet_with_language_having_no_project_if_language_is_in_filter() {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81d)), null, asList("<null>", "java"));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81)), null, asList("java"));
    insertMetrics(COVERAGE, NCLOC_LANGUAGE_DISTRIBUTION_KEY);

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
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java").setTags(newArrayList("finance", "platform")));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown").setTags(singletonList("offshore")));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube").setTags(newArrayList("offshore")));

    SearchProjectsWsResponse result = call(request.setFacets(singletonList(FILTER_TAGS)));

    Common.Facet facet = result.getFacets().getFacetsList().stream()
      .filter(oneFacet -> FILTER_TAGS.equals(oneFacet.getProperty()))
      .findFirst().orElseThrow(IllegalStateException::new);
    assertThat(facet.getProperty()).isEqualTo(FILTER_TAGS);
    assertThat(facet.getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsExactly(
        tuple("offshore", 2L),
        tuple("finance", 1L),
        tuple("platform", 1L));
  }

  @Test
  public void return_tags_facet_with_tags_having_no_project_if_tags_is_in_filter() {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java").setTags(newArrayList("finance", "platform")));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown").setTags(singletonList("offshore")));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube").setTags(newArrayList("offshore")));

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
  public void default_sort_is_by_ascending_name() throws Exception {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 500_001d)));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName).containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown", "Sonar Qube");
  }

  @Test
  public void sort_by_name() throws Exception {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 500_001d)));

    assertThat(call(request.setSort("name").setAsc(true)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown", "Sonar Qube");
    assertThat(call(request.setSort("name").setAsc(false)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Qube", "Sonar Markdown", "Sonar Java", "Sonar Groovy");
  }

  @Test
  public void sort_by_coverage() throws Exception {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), newArrayList(newMeasure(COVERAGE, 81), newMeasure(NCLOC, 5d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d)));
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube"), newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 500_001d)));
    insertMetrics(COVERAGE);

    assertThat(call(request.setSort(COVERAGE).setAsc(true)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Markdown", "Sonar Qube", "Sonar Groovy", "Sonar Java");
    assertThat(call(request.setSort(COVERAGE).setAsc(false)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Groovy", "Sonar Java", "Sonar Markdown", "Sonar Qube");
  }

  @Test
  public void sort_by_quality_gate() throws Exception {
    OrganizationDto organization = db.getDefaultOrganization();
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Java"), "ERROR");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Groovy"), "WARN");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Markdown"), "OK");
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organization).setName("Sonar Qube"), "OK");
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(QUALITY_GATE_STATUS).setValueType(LEVEL.name()).setEnabled(true).setHidden(false));
    db.commit();

    assertThat(call(request.setSort(QUALITY_GATE_STATUS).setAsc(true)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Markdown", "Sonar Qube", "Sonar Groovy", "Sonar Java");
    assertThat(call(request.setSort(QUALITY_GATE_STATUS).setAsc(false)).getComponentsList()).extracting(Component::getName)
      .containsExactly("Sonar Java", "Sonar Groovy", "Sonar Markdown", "Sonar Qube");
  }

  @Test
  public void return_last_analysis_date() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project1 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto));
    db.components().insertSnapshot(newAnalysis(project1).setCreatedAt(10_000_000_000L).setLast(false));
    db.components().insertSnapshot(newAnalysis(project1).setCreatedAt(20_000_000_000L).setLast(true));
    ComponentDto project2 = insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto));
    db.components().insertSnapshot(newAnalysis(project2).setCreatedAt(30_000_000_000L).setLast(true));
    // No snapshot on project 3
    insertProjectInDbAndEs(ComponentTesting.newPrivateProjectDto(organizationDto));

    SearchProjectsWsResponse result = call(request.setAdditionalFields(singletonList("analysisDate")));

    assertThat(result.getComponentsList()).extracting(Component::getAnalysisDate)
      .containsOnly(formatDateTime(new Date(20_000_000_000L)), formatDateTime(new Date(30_000_000_000L)), "");
  }

  @Test
  public void return_visibility_flag() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto privateProject = insertProjectInDbAndEs(newPrivateProjectDto(organization));
    ComponentDto publicProject = insertProjectInDbAndEs(newPublicProjectDto(organization));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getVisibility)
      .containsExactly(
              tuple(privateProject.getKey(), privateProject.isPrivate() ? "private" : "public"),
              tuple(publicProject.getKey(), publicProject.isPrivate() ? "private" : "public"));
  }

  @Test
  public void fail_when_metrics_are_unknown() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown metric(s) [coverage, debt]");

    request.setFilter("coverage > 80").setSort("debt");

    call(request);
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    expectedException.expect(IllegalArgumentException.class);

    call(request.setPageSize(501));
  }

  private SearchProjectsWsResponse call(SearchProjectsRequest.Builder requestBuilder) {
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

  private ComponentDto insertProjectInDbAndEs(ComponentDto project) {
    return insertProjectInDbAndEs(project, emptyList());
  }

  private ComponentDto insertProjectInDbAndEs(ComponentDto project, List<Map<String, Object>> measures) {
    return insertProjectInDbAndEs(project, measures, null, emptyList());
  }

  private ComponentDto insertProjectInDbAndEs(ComponentDto project, String qualityGateStatus) {
    return insertProjectInDbAndEs(project, emptyList(), qualityGateStatus, emptyList());
  }

  private ComponentDto insertProjectInDbAndEs(ComponentDto project, List<Map<String, Object>> measures, @Nullable String qualityGateStatus,
    List<String> languages) {
    ComponentDto res = componentDb.insertComponent(project);
    try {
      es.putDocuments(INDEX_TYPE_PROJECT_MEASURES,
        new ProjectMeasuresDoc()
          .setOrganizationUuid(project.getOrganizationUuid())
          .setId(project.uuid())
          .setKey(project.key())
          .setName(project.name())
          .setMeasures(measures)
          .setQualityGateStatus(qualityGateStatus)
          .setLanguages(languages)
          .setTags(project.getTags()));
      authorizationIndexerTester.allowOnlyAnyone(project);
    } catch (Exception e) {
      Throwables.propagate(e);
    }

    return res;
  }

  private void insertMetrics(String... metricKeys) {
    for (String metricKey : metricKeys) {
      dbClient.metricDao().insert(dbSession, newMetricDto().setKey(metricKey).setValueType(INT.name()).setEnabled(true).setHidden(false));
    }
    dbSession.commit();
  }

  private static Map<String, Object> newMeasure(String key, Object value) {
    return ImmutableMap.of("key", key, "value", value);
  }

  private void addFavourite(ComponentDto project) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("favourite").setResourceId(project.getId()).setUserId(userSession.getUserId()));
  }
}
