/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.measure.index;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ws.FilterParser.Operator;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerDao;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;

@RunWith(DataProviderRunner.class)
public class ProjectMeasuresIndexTest {

  private static final String MAINTAINABILITY_RATING = "sqale_rating";
  private static final String NEW_MAINTAINABILITY_RATING_KEY = "new_maintainability_rating";
  private static final String RELIABILITY_RATING = "reliability_rating";
  private static final String NEW_RELIABILITY_RATING = "new_reliability_rating";
  private static final String SECURITY_RATING = "security_rating";
  private static final String NEW_SECURITY_RATING = "new_security_rating";
  private static final String COVERAGE = "coverage";
  private static final String NEW_COVERAGE = "new_coverage";
  private static final String DUPLICATION = "duplicated_lines_density";
  private static final String NEW_DUPLICATION = "new_duplicated_lines_density";
  private static final String NCLOC = "ncloc";
  private static final String NEW_LINES = "new_lines";
  private static final String LANGUAGES = "languages";

  private static final OrganizationDto ORG = OrganizationTesting.newOrganizationDto();
  private static final ComponentDto PROJECT1 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("Project-1").setName("Project 1").setDbKey("key-1");
  private static final ComponentDto PROJECT2 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("Project-2").setName("Project 2").setDbKey("key-2");
  private static final ComponentDto PROJECT3 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("Project-3").setName("Project 3").setDbKey("key-3");
  private static final UserDto USER1 = newUserDto();
  private static final UserDto USER2 = newUserDto();
  private static final GroupDto GROUP1 = newGroupDto();
  private static final GroupDto GROUP2 = newGroupDto();

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @DataProvider
  public static Object[][] rating_metric_keys() {
    return new Object[][] {{MAINTAINABILITY_RATING}, {NEW_MAINTAINABILITY_RATING_KEY}, {RELIABILITY_RATING}, {NEW_RELIABILITY_RATING}, {SECURITY_RATING}, {NEW_SECURITY_RATING}};
  }

  private ProjectMeasuresIndexer projectMeasureIndexer = new ProjectMeasuresIndexer(null, es.client());
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, projectMeasureIndexer);
  private ProjectMeasuresIndex underTest = new ProjectMeasuresIndex(es.client(), new AuthorizationTypeSupport(userSession), System2.INSTANCE);

  @Test
  public void return_empty_if_no_projects() {
    assertNoResults(new ProjectMeasuresQuery());
  }

  @Test
  public void default_sort_is_by_ascending_case_insensitive_name_then_by_key() {
    ComponentDto windows = ComponentTesting.newPrivateProjectDto(ORG).setUuid("windows").setName("Windows").setDbKey("project1");
    ComponentDto apachee = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apachee").setName("apachee").setDbKey("project2");
    ComponentDto apache1 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-1").setName("Apache").setDbKey("project3");
    ComponentDto apache2 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-2").setName("Apache").setDbKey("project4");
    index(newDoc(windows), newDoc(apachee), newDoc(apache1), newDoc(apache2));

    assertResults(new ProjectMeasuresQuery(), apache1, apache2, apachee, windows);
  }

  @Test
  public void sort_by_insensitive_name() {
    ComponentDto windows = ComponentTesting.newPrivateProjectDto(ORG).setUuid("windows").setName("Windows");
    ComponentDto apachee = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apachee").setName("apachee");
    ComponentDto apache = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache").setName("Apache");
    index(newDoc(windows), newDoc(apachee), newDoc(apache));

    assertResults(new ProjectMeasuresQuery().setSort("name").setAsc(true), apache, apachee, windows);
    assertResults(new ProjectMeasuresQuery().setSort("name").setAsc(false), windows, apachee, apache);
  }

  @Test
  public void sort_by_ncloc() {
    index(
      newDoc(PROJECT1, NCLOC, 15_000d),
      newDoc(PROJECT2, NCLOC, 30_000d),
      newDoc(PROJECT3, NCLOC, 1_000d));

    assertResults(new ProjectMeasuresQuery().setSort("ncloc").setAsc(true), PROJECT3, PROJECT1, PROJECT2);
    assertResults(new ProjectMeasuresQuery().setSort("ncloc").setAsc(false), PROJECT2, PROJECT1, PROJECT3);
  }

  @Test
  public void sort_by_a_metric_then_by_name_then_by_key() {
    ComponentDto windows = ComponentTesting.newPrivateProjectDto(ORG).setUuid("windows").setName("Windows").setDbKey("project1");
    ComponentDto apachee = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apachee").setName("apachee").setDbKey("project2");
    ComponentDto apache1 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-1").setName("Apache").setDbKey("project3");
    ComponentDto apache2 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-2").setName("Apache").setDbKey("project4");
    index(
      newDoc(windows, NCLOC, 10_000d),
      newDoc(apachee, NCLOC, 5_000d),
      newDoc(apache1, NCLOC, 5_000d),
      newDoc(apache2, NCLOC, 5_000d));

    assertResults(new ProjectMeasuresQuery().setSort("ncloc").setAsc(true), apache1, apache2, apachee, windows);
    assertResults(new ProjectMeasuresQuery().setSort("ncloc").setAsc(false), windows, apache1, apache2, apachee);
  }

  @Test
  public void sort_by_quality_gate_status() {
    ComponentDto project4 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("Project-4").setName("Project 4").setDbKey("key-4");
    index(
      newDoc(PROJECT1).setQualityGateStatus(OK.name()),
      newDoc(PROJECT2).setQualityGateStatus(ERROR.name()),
      newDoc(PROJECT3).setQualityGateStatus(WARN.name()),
      newDoc(project4).setQualityGateStatus(OK.name()));

    assertResults(new ProjectMeasuresQuery().setSort("alert_status").setAsc(true), PROJECT1, project4, PROJECT3, PROJECT2);
    assertResults(new ProjectMeasuresQuery().setSort("alert_status").setAsc(false), PROJECT2, PROJECT3, PROJECT1, project4);
  }

  @Test
  public void sort_by_quality_gate_status_then_by_name_then_by_key() {
    ComponentDto windows = ComponentTesting.newPrivateProjectDto(ORG).setUuid("windows").setName("Windows").setDbKey("project1");
    ComponentDto apachee = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apachee").setName("apachee").setDbKey("project2");
    ComponentDto apache1 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-1").setName("Apache").setDbKey("project3");
    ComponentDto apache2 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-2").setName("Apache").setDbKey("project4");
    index(
      newDoc(windows).setQualityGateStatus(WARN.name()),
      newDoc(apachee).setQualityGateStatus(OK.name()),
      newDoc(apache1).setQualityGateStatus(OK.name()),
      newDoc(apache2).setQualityGateStatus(OK.name()));

    assertResults(new ProjectMeasuresQuery().setSort("alert_status").setAsc(true), apache1, apache2, apachee, windows);
    assertResults(new ProjectMeasuresQuery().setSort("alert_status").setAsc(false), windows, apache1, apache2, apachee);
  }

  @Test
  public void paginate_results() {
    IntStream.rangeClosed(1, 9)
      .forEach(i -> index(newDoc(newPrivateProjectDto(ORG, "P" + i))));

    SearchIdResult<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().setPage(2, 3));

    assertThat(result.getIds()).containsExactly("P4", "P5", "P6");
    assertThat(result.getTotal()).isEqualTo(9);
  }

  @Test
  public void filter_with_lower_than() {
    index(
      newDoc(PROJECT1, COVERAGE, 79d, NCLOC, 10_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_000d),
      newDoc(PROJECT3, COVERAGE, 81d, NCLOC, 10_000d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LT, 80d));

    assertResults(query, PROJECT1);
  }

  @Test
  public void filter_with_lower_than_or_equals() {
    index(
      newDoc(PROJECT1, COVERAGE, 79d, NCLOC, 10_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_000d),
      newDoc(PROJECT3, COVERAGE, 81d, NCLOC, 10_000d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LTE, 80d));

    assertResults(query, PROJECT1, PROJECT2);
  }

  @Test
  public void filter_with_greater_than() {
    index(
      newDoc(PROJECT1, COVERAGE, 80d, NCLOC, 30_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 30_001d),
      newDoc(PROJECT3, COVERAGE, 80d, NCLOC, 30_001d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.create(NCLOC, Operator.GT, 30_000d));
    assertResults(query, PROJECT2, PROJECT3);

    query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.create(NCLOC, Operator.GT, 100_000d));
    assertNoResults(query);
  }

  @Test
  public void filter_with_greater_than_or_equals() {
    index(
      newDoc(PROJECT1, COVERAGE, 80d, NCLOC, 30_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 30_001d),
      newDoc(PROJECT3, COVERAGE, 80d, NCLOC, 30_001d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.create(NCLOC, Operator.GTE, 30_001d));
    assertResults(query, PROJECT2, PROJECT3);

    query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.create(NCLOC, Operator.GTE, 100_000d));
    assertNoResults(query);
  }

  @Test
  public void filter_with_equals() {
    index(
      newDoc(PROJECT1, COVERAGE, 79d, NCLOC, 10_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_000d),
      newDoc(PROJECT3, COVERAGE, 81d, NCLOC, 10_000d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.EQ, 80d));

    assertResults(query, PROJECT2);
  }

  @Test
  public void filter_on_no_data_with_several_projects() {
    index(
      newDoc(PROJECT1, NCLOC, 1d),
      newDoc(PROJECT2, DUPLICATION, 80d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.createNoData(DUPLICATION));

    assertResults(query, PROJECT1);
  }

  @Test
  public void filter_on_no_data_should_not_return_projects_with_data_and_other_measures() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(ORG);
    index(newDoc(project, DUPLICATION, 80d, NCLOC, 1d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.createNoData(DUPLICATION));

    assertNoResults(query);
  }

  @Test
  public void filter_on_no_data_should_not_return_projects_with_data() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(ORG);
    index(newDoc(project, DUPLICATION, 80d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.createNoData(DUPLICATION));

    assertNoResults(query);
  }

  @Test
  public void filter_on_no_data_should_return_projects_with_no_data() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(ORG);
    index(newDoc(project, NCLOC, 1d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(MetricCriterion.createNoData(DUPLICATION));

    assertResults(query, project);
  }

  @Test
  public void filter_on_several_metrics() {
    index(
      newDoc(PROJECT1, COVERAGE, 81d, NCLOC, 10_001d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_001d),
      newDoc(PROJECT3, COVERAGE, 79d, NCLOC, 10_000d));

    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LTE, 80d))
      .addMetricCriterion(MetricCriterion.create(NCLOC, Operator.GT, 10_000d))
      .addMetricCriterion(MetricCriterion.create(NCLOC, Operator.LT, 11_000d));
    assertResults(esQuery, PROJECT2);
  }

  @Test
  public void filter_on_quality_gate_status() {
    index(
      newDoc(PROJECT1).setQualityGateStatus(OK.name()),
      newDoc(PROJECT2).setQualityGateStatus(OK.name()),
      newDoc(PROJECT3).setQualityGateStatus(WARN.name()));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().setQualityGateStatus(OK);
    assertResults(query, PROJECT1, PROJECT2);
  }

  @Test
  public void filter_on_languages() {
    ComponentDto project4 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("Project-4").setName("Project 4").setDbKey("key-4");
    index(
      newDoc(PROJECT1).setLanguages(singletonList("java")),
      newDoc(PROJECT2).setLanguages(singletonList("xoo")),
      newDoc(PROJECT3).setLanguages(singletonList("xoo")),
      newDoc(project4).setLanguages(asList("<null>", "java", "xoo")));

    assertResults(new ProjectMeasuresQuery().setLanguages(newHashSet("java", "xoo")), PROJECT1, PROJECT2, PROJECT3, project4);
    assertResults(new ProjectMeasuresQuery().setLanguages(newHashSet("java")), PROJECT1, project4);
    assertResults(new ProjectMeasuresQuery().setLanguages(newHashSet("unknown")));
  }

  @Test
  public void filter_on_query_text() {
    ComponentDto windows = ComponentTesting.newPrivateProjectDto(ORG).setUuid("windows").setName("Windows").setDbKey("project1");
    ComponentDto apachee = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apachee").setName("apachee").setDbKey("project2");
    ComponentDto apache1 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-1").setName("Apache").setDbKey("project3");
    ComponentDto apache2 = ComponentTesting.newPrivateProjectDto(ORG).setUuid("apache-2").setName("Apache").setDbKey("project4");
    index(newDoc(windows), newDoc(apachee), newDoc(apache1), newDoc(apache2));

    assertResults(new ProjectMeasuresQuery().setQueryText("windows"), windows);
    assertResults(new ProjectMeasuresQuery().setQueryText("project2"), apachee);
    assertResults(new ProjectMeasuresQuery().setQueryText("pAch"), apache1, apache2, apachee);
  }

  @Test
  public void filter_on_ids() {
    index(
      newDoc(PROJECT1),
      newDoc(PROJECT2),
      newDoc(PROJECT3));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().setProjectUuids(newHashSet(PROJECT1.uuid(), PROJECT3.uuid()));
    assertResults(query, PROJECT1, PROJECT3);
  }

  @Test
  public void filter_on_tags() {
    index(
      newDoc(PROJECT1).setTags(newArrayList("finance", "platform")),
      newDoc(PROJECT2).setTags(newArrayList("marketing", "platform")),
      newDoc(PROJECT3).setTags(newArrayList("finance", "language")));

    assertResults(new ProjectMeasuresQuery().setTags(newHashSet("finance")), PROJECT1, PROJECT3);
    assertResults(new ProjectMeasuresQuery().setTags(newHashSet("finance", "language")), PROJECT1, PROJECT3);
    assertResults(new ProjectMeasuresQuery().setTags(newHashSet("finance", "marketing")), PROJECT1, PROJECT2, PROJECT3);
    assertResults(new ProjectMeasuresQuery().setTags(newHashSet("marketing")), PROJECT2);
    assertNoResults(new ProjectMeasuresQuery().setTags(newHashSet("tag 42")));
  }

  @Test
  public void filter_on_organization() {
    OrganizationDto org1 = OrganizationTesting.newOrganizationDto();
    OrganizationDto org2 = OrganizationTesting.newOrganizationDto();
    ComponentDto projectInOrg1 = ComponentTesting.newPrivateProjectDto(org1);
    ComponentDto projectInOrg2 = ComponentTesting.newPrivateProjectDto(org2);
    index(newDoc(projectInOrg1), newDoc(projectInOrg2));

    ProjectMeasuresQuery query1 = new ProjectMeasuresQuery().setOrganizationUuid(org1.getUuid());
    assertResults(query1, projectInOrg1);

    ProjectMeasuresQuery query2 = new ProjectMeasuresQuery().setOrganizationUuid(org2.getUuid());
    assertResults(query2, projectInOrg2);

    ProjectMeasuresQuery query3 = new ProjectMeasuresQuery().setOrganizationUuid("another_org");
    assertNoResults(query3);
  }

  @Test
  public void return_only_projects_authorized_for_user() {
    indexForUser(USER1, newDoc(PROJECT1), newDoc(PROJECT2));
    indexForUser(USER2, newDoc(PROJECT3));

    userSession.logIn(USER1);
    assertResults(new ProjectMeasuresQuery(), PROJECT1, PROJECT2);
  }

  @Test
  public void return_only_projects_authorized_for_user_groups() {
    indexForGroup(GROUP1, newDoc(PROJECT1), newDoc(PROJECT2));
    indexForGroup(GROUP2, newDoc(PROJECT3));

    userSession.logIn().setGroups(GROUP1);
    assertResults(new ProjectMeasuresQuery(), PROJECT1, PROJECT2);
  }

  @Test
  public void return_only_projects_authorized_for_user_and_groups() {
    indexForUser(USER1, newDoc(PROJECT1), newDoc(PROJECT2));
    indexForGroup(GROUP1, newDoc(PROJECT3));

    userSession.logIn(USER1).setGroups(GROUP1);
    assertResults(new ProjectMeasuresQuery(), PROJECT1, PROJECT2, PROJECT3);
  }

  @Test
  public void anonymous_user_can_only_access_projects_authorized_for_anyone() {
    index(newDoc(PROJECT1));
    indexForUser(USER1, newDoc(PROJECT2));

    userSession.anonymous();
    assertResults(new ProjectMeasuresQuery(), PROJECT1);
  }

  @Test
  public void root_user_can_access_all_projects() {
    indexForUser(USER1, newDoc(PROJECT1));
    // connecting with a root but not USER1
    userSession.logIn().setRoot();

    assertResults(new ProjectMeasuresQuery(), PROJECT1);
  }

  @Test
  public void does_not_return_facet_when_no_facets_in_options() {
    index(
      newDoc(PROJECT1, NCLOC, 10d, COVERAGE_KEY, 30d, MAINTAINABILITY_RATING, 3d)
        .setQualityGateStatus(OK.name()));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.getAll()).isEmpty();
  }

  @Test
  public void facet_ncloc() {
    index(
      // 3 docs with ncloc<1K
      newDoc(NCLOC, 0d),
      newDoc(NCLOC, 0d),
      newDoc(NCLOC, 999d),
      // 2 docs with ncloc>=1K and ncloc<10K
      newDoc(NCLOC, 1_000d),
      newDoc(NCLOC, 9_999d),
      // 4 docs with ncloc>=10K and ncloc<100K
      newDoc(NCLOC, 10_000d),
      newDoc(NCLOC, 10_000d),
      newDoc(NCLOC, 11_000d),
      newDoc(NCLOC, 99_000d),
      // 2 docs with ncloc>=100K and ncloc<500K
      newDoc(NCLOC, 100_000d),
      newDoc(NCLOC, 499_000d),
      // 5 docs with ncloc>= 500K
      newDoc(NCLOC, 500_000d),
      newDoc(NCLOC, 100_000_000d),
      newDoc(NCLOC, 500_000d),
      newDoc(NCLOC, 1_000_000d),
      newDoc(NCLOC, 100_000_000_000d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(NCLOC)).getFacets();

    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 3L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 4L),
      entry("100000.0-500000.0", 2L),
      entry("500000.0-*", 5L));
  }

  @Test
  public void facet_ncloc_is_sticky() {
    index(
      // 1 docs with ncloc<1K
      newDoc(NCLOC, 999d, COVERAGE, 0d, DUPLICATION, 0d),
      // 2 docs with ncloc>=1K and ncloc<10K
      newDoc(NCLOC, 1_000d, COVERAGE, 10d, DUPLICATION, 0d),
      newDoc(NCLOC, 9_999d, COVERAGE, 20d, DUPLICATION, 0d),
      // 3 docs with ncloc>=10K and ncloc<100K
      newDoc(NCLOC, 10_000d, COVERAGE, 31d, DUPLICATION, 0d),
      newDoc(NCLOC, 11_000d, COVERAGE, 40d, DUPLICATION, 0d),
      newDoc(NCLOC, 99_000d, COVERAGE, 50d, DUPLICATION, 0d),
      // 2 docs with ncloc>=100K and ncloc<500K
      newDoc(NCLOC, 100_000d, COVERAGE, 71d, DUPLICATION, 0d),
      newDoc(NCLOC, 499_000d, COVERAGE, 80d, DUPLICATION, 0d),
      // 1 docs with ncloc>= 500K
      newDoc(NCLOC, 501_000d, COVERAGE, 81d, DUPLICATION, 20d));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(NCLOC, Operator.LT, 10_000d))
      .addMetricCriterion(MetricCriterion.create(DUPLICATION, Operator.LT, 10d)),
      new SearchOptions().addFacets(NCLOC, COVERAGE)).getFacets();

    // Sticky facet on ncloc does not take into account ncloc filter
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 1L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 3L),
      entry("100000.0-500000.0", 2L),
      entry("500000.0-*", 0L));
    // But facet on coverage does well take into into filters
    assertThat(facets.get(COVERAGE)).containsOnly(
      entry("NO_DATA", 0L),
      entry("*-30.0", 3L),
      entry("30.0-50.0", 0L),
      entry("50.0-70.0", 0L),
      entry("70.0-80.0", 0L),
      entry("80.0-*", 0L));
  }

  @Test
  public void facet_ncloc_contains_only_projects_authorized_for_user() {
    // User can see these projects
    indexForUser(USER1,
      // docs with ncloc<1K
      newDoc(NCLOC, 0d),
      newDoc(NCLOC, 100d),
      newDoc(NCLOC, 999d),
      // docs with ncloc>=1K and ncloc<10K
      newDoc(NCLOC, 1_000d),
      newDoc(NCLOC, 9_999d));

    // User cannot see these projects
    indexForUser(USER2,
      // doc with ncloc>=10K and ncloc<100K
      newDoc(NCLOC, 11_000d),
      // doc with ncloc>=100K and ncloc<500K
      newDoc(NCLOC, 499_000d),
      // doc with ncloc>= 500K
      newDoc(NCLOC, 501_000d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(NCLOC)).getFacets();

    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 3L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 0L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  public void facet_new_lines() {
    index(
      // 3 docs with ncloc<1K
      newDoc(NEW_LINES, 0d),
      newDoc(NEW_LINES, 0d),
      newDoc(NEW_LINES, 999d),
      // 2 docs with ncloc>=1K and ncloc<10K
      newDoc(NEW_LINES, 1_000d),
      newDoc(NEW_LINES, 9_999d),
      // 4 docs with ncloc>=10K and ncloc<100K
      newDoc(NEW_LINES, 10_000d),
      newDoc(NEW_LINES, 10_000d),
      newDoc(NEW_LINES, 11_000d),
      newDoc(NEW_LINES, 99_000d),
      // 2 docs with ncloc>=100K and ncloc<500K
      newDoc(NEW_LINES, 100_000d),
      newDoc(NEW_LINES, 499_000d),
      // 5 docs with ncloc>= 500K
      newDoc(NEW_LINES, 500_000d),
      newDoc(NEW_LINES, 100_000_000d),
      newDoc(NEW_LINES, 500_000d),
      newDoc(NEW_LINES, 1_000_000d),
      newDoc(NEW_LINES, 100_000_000_000d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(NEW_LINES)).getFacets();

    assertThat(facets.get(NEW_LINES)).containsExactly(
      entry("*-1000.0", 3L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 4L),
      entry("100000.0-500000.0", 2L),
      entry("500000.0-*", 5L));
  }

  @Test
  public void facet_coverage() {
    index(
      // 1 doc with no coverage
      newDocWithNoMeasure(),
      // 3 docs with coverage<30%
      newDoc(COVERAGE, 0d),
      newDoc(COVERAGE, 0d),
      newDoc(COVERAGE, 29d),
      // 2 docs with coverage>=30% and coverage<50%
      newDoc(COVERAGE, 30d),
      newDoc(COVERAGE, 49d),
      // 4 docs with coverage>=50% and coverage<70%
      newDoc(COVERAGE, 50d),
      newDoc(COVERAGE, 60d),
      newDoc(COVERAGE, 60d),
      newDoc(COVERAGE, 69d),
      // 2 docs with coverage>=70% and coverage<80%
      newDoc(COVERAGE, 70d),
      newDoc(COVERAGE, 79d),
      // 5 docs with coverage>= 80%
      newDoc(COVERAGE, 80d),
      newDoc(COVERAGE, 80d),
      newDoc(COVERAGE, 90d),
      newDoc(COVERAGE, 90.5d),
      newDoc(COVERAGE, 100d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(COVERAGE)).getFacets();

    assertThat(facets.get(COVERAGE)).containsOnly(
      entry("NO_DATA", 1L),
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 4L),
      entry("70.0-80.0", 2L),
      entry("80.0-*", 5L));
  }

  @Test
  public void facet_coverage_is_sticky() {
    index(
      // docs with no coverage
      newDoc(NCLOC, 999d, DUPLICATION, 0d),
      newDoc(NCLOC, 999d, DUPLICATION, 1d),
      newDoc(NCLOC, 999d, DUPLICATION, 20d),
      // docs with coverage<30%
      newDoc(NCLOC, 999d, COVERAGE, 0d, DUPLICATION, 0d),
      newDoc(NCLOC, 1_000d, COVERAGE, 10d, DUPLICATION, 0d),
      newDoc(NCLOC, 9_999d, COVERAGE, 20d, DUPLICATION, 0d),
      // docs with coverage>=30% and coverage<50%
      newDoc(NCLOC, 10_000d, COVERAGE, 31d, DUPLICATION, 0d),
      newDoc(NCLOC, 11_000d, COVERAGE, 40d, DUPLICATION, 0d),
      // docs with coverage>=50% and coverage<70%
      newDoc(NCLOC, 99_000d, COVERAGE, 50d, DUPLICATION, 0d),
      // docs with coverage>=70% and coverage<80%
      newDoc(NCLOC, 100_000d, COVERAGE, 71d, DUPLICATION, 0d),
      // docs with coverage>= 80%
      newDoc(NCLOC, 499_000d, COVERAGE, 80d, DUPLICATION, 15d),
      newDoc(NCLOC, 501_000d, COVERAGE, 810d, DUPLICATION, 20d));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LT, 30d))
      .addMetricCriterion(MetricCriterion.create(DUPLICATION, Operator.LT, 10d)),
      new SearchOptions().addFacets(COVERAGE, NCLOC)).getFacets();

    // Sticky facet on coverage does not take into account coverage filter
    assertThat(facets.get(COVERAGE)).containsExactly(
      entry("NO_DATA", 2L),
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 1L),
      entry("70.0-80.0", 1L),
      entry("80.0-*", 0L));
    // But facet on ncloc does well take into into filters
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 1L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 0L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  public void facet_coverage_contains_only_projects_authorized_for_user() {
    // User can see these projects
    indexForUser(USER1,
      // 1 doc with no coverage
      newDocWithNoMeasure(),
      // docs with coverage<30%
      newDoc(COVERAGE, 0d),
      newDoc(COVERAGE, 0d),
      newDoc(COVERAGE, 29d),
      // docs with coverage>=30% and coverage<50%
      newDoc(COVERAGE, 30d),
      newDoc(COVERAGE, 49d));

    // User cannot see these projects
    indexForUser(USER2,
      // 2 docs with no coverage
      newDocWithNoMeasure(),
      newDocWithNoMeasure(),
      // docs with coverage>=50% and coverage<70%
      newDoc(COVERAGE, 50d),
      // docs with coverage>=70% and coverage<80%
      newDoc(COVERAGE, 70d),
      // docs with coverage>= 80%
      newDoc(COVERAGE, 80d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(COVERAGE)).getFacets();

    assertThat(facets.get(COVERAGE)).containsExactly(
      entry("NO_DATA", 1L),
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 0L),
      entry("70.0-80.0", 0L),
      entry("80.0-*", 0L));
  }

  @Test
  public void facet_new_coverage() {
    index(
      // 1 doc with no coverage
      newDocWithNoMeasure(),
      // 3 docs with coverage<30%
      newDoc(NEW_COVERAGE, 0d),
      newDoc(NEW_COVERAGE, 0d),
      newDoc(NEW_COVERAGE, 29d),
      // 2 docs with coverage>=30% and coverage<50%
      newDoc(NEW_COVERAGE, 30d),
      newDoc(NEW_COVERAGE, 49d),
      // 4 docs with coverage>=50% and coverage<70%
      newDoc(NEW_COVERAGE, 50d),
      newDoc(NEW_COVERAGE, 60d),
      newDoc(NEW_COVERAGE, 60d),
      newDoc(NEW_COVERAGE, 69d),
      // 2 docs with coverage>=70% and coverage<80%
      newDoc(NEW_COVERAGE, 70d),
      newDoc(NEW_COVERAGE, 79d),
      // 5 docs with coverage>= 80%
      newDoc(NEW_COVERAGE, 80d),
      newDoc(NEW_COVERAGE, 80d),
      newDoc(NEW_COVERAGE, 90d),
      newDoc(NEW_COVERAGE, 90.5d),
      newDoc(NEW_COVERAGE, 100d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(NEW_COVERAGE)).getFacets();

    assertThat(facets.get(NEW_COVERAGE)).containsOnly(
      entry("NO_DATA", 1L),
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 4L),
      entry("70.0-80.0", 2L),
      entry("80.0-*", 5L));
  }

  @Test
  public void facet_duplicated_lines_density() {
    index(
      // 1 doc with no duplication
      newDocWithNoMeasure(),
      // 3 docs with duplication<3%
      newDoc(DUPLICATION, 0d),
      newDoc(DUPLICATION, 0d),
      newDoc(DUPLICATION, 2.9d),
      // 2 docs with duplication>=3% and duplication<5%
      newDoc(DUPLICATION, 3d),
      newDoc(DUPLICATION, 4.9d),
      // 4 docs with duplication>=5% and duplication<10%
      newDoc(DUPLICATION, 5d),
      newDoc(DUPLICATION, 6d),
      newDoc(DUPLICATION, 6d),
      newDoc(DUPLICATION, 9.9d),
      // 2 docs with duplication>=10% and duplication<20%
      newDoc(DUPLICATION, 10d),
      newDoc(DUPLICATION, 19.9d),
      // 5 docs with duplication>= 20%
      newDoc(DUPLICATION, 20d),
      newDoc(DUPLICATION, 20d),
      newDoc(DUPLICATION, 50d),
      newDoc(DUPLICATION, 80d),
      newDoc(DUPLICATION, 100d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(DUPLICATION)).getFacets();

    assertThat(facets.get(DUPLICATION)).containsOnly(
      entry("NO_DATA", 1L),
      entry("*-3.0", 3L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 4L),
      entry("10.0-20.0", 2L),
      entry("20.0-*", 5L));
  }

  @Test
  public void facet_duplicated_lines_density_is_sticky() {
    index(
      // docs with no duplication
      newDoc(NCLOC, 50_001d, COVERAGE, 29d),
      // docs with duplication<3%
      newDoc(DUPLICATION, 0d, NCLOC, 999d, COVERAGE, 0d),
      // docs with duplication>=3% and duplication<5%
      newDoc(DUPLICATION, 3d, NCLOC, 5000d, COVERAGE, 0d),
      newDoc(DUPLICATION, 4.9d, NCLOC, 6000d, COVERAGE, 0d),
      // docs with duplication>=5% and duplication<10%
      newDoc(DUPLICATION, 5d, NCLOC, 11000d, COVERAGE, 0d),
      // docs with duplication>=10% and duplication<20%
      newDoc(DUPLICATION, 10d, NCLOC, 120000d, COVERAGE, 10d),
      newDoc(DUPLICATION, 19.9d, NCLOC, 130000d, COVERAGE, 20d),
      // docs with duplication>= 20%
      newDoc(DUPLICATION, 20d, NCLOC, 1000000d, COVERAGE, 40d));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(DUPLICATION, Operator.LT, 10d))
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LT, 30d)),
      new SearchOptions().addFacets(DUPLICATION, NCLOC)).getFacets();

    // Sticky facet on duplication does not take into account duplication filter
    assertThat(facets.get(DUPLICATION)).containsOnly(
      entry("NO_DATA", 1L),
      entry("*-3.0", 1L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 1L),
      entry("10.0-20.0", 2L),
      entry("20.0-*", 0L));
    // But facet on ncloc does well take into into filters
    assertThat(facets.get(NCLOC)).containsOnly(
      entry("*-1000.0", 1L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 1L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  public void facet_duplicated_lines_density_contains_only_projects_authorized_for_user() {
    // User can see these projects
    indexForUser(USER1,
      // docs with no duplication
      newDocWithNoMeasure(),
      // docs with duplication<3%
      newDoc(DUPLICATION, 0d),
      newDoc(DUPLICATION, 0d),
      newDoc(DUPLICATION, 2.9d),
      // docs with duplication>=3% and duplication<5%
      newDoc(DUPLICATION, 3d),
      newDoc(DUPLICATION, 4.9d));

    // User cannot see these projects
    indexForUser(USER2,
      // docs with no duplication
      newDocWithNoMeasure(),
      newDocWithNoMeasure(),
      // docs with duplication>=5% and duplication<10%
      newDoc(DUPLICATION, 5d),
      // docs with duplication>=10% and duplication<20%
      newDoc(DUPLICATION, 10d),
      // docs with duplication>= 20%
      newDoc(DUPLICATION, 20d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(DUPLICATION)).getFacets();

    assertThat(facets.get(DUPLICATION)).containsOnly(
      entry("NO_DATA", 1L),
      entry("*-3.0", 3L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 0L),
      entry("10.0-20.0", 0L),
      entry("20.0-*", 0L));
  }

  @Test
  public void facet_new_duplicated_lines_density() {
    index(
      // 2 docs with no measure
      newDocWithNoMeasure(),
      newDocWithNoMeasure(),
      // 3 docs with duplication<3%
      newDoc(NEW_DUPLICATION, 0d),
      newDoc(NEW_DUPLICATION, 0d),
      newDoc(NEW_DUPLICATION, 2.9d),
      // 2 docs with duplication>=3% and duplication<5%
      newDoc(NEW_DUPLICATION, 3d),
      newDoc(NEW_DUPLICATION, 4.9d),
      // 4 docs with duplication>=5% and duplication<10%
      newDoc(NEW_DUPLICATION, 5d),
      newDoc(NEW_DUPLICATION, 6d),
      newDoc(NEW_DUPLICATION, 6d),
      newDoc(NEW_DUPLICATION, 9.9d),
      // 2 docs with duplication>=10% and duplication<20%
      newDoc(NEW_DUPLICATION, 10d),
      newDoc(NEW_DUPLICATION, 19.9d),
      // 5 docs with duplication>= 20%
      newDoc(NEW_DUPLICATION, 20d),
      newDoc(NEW_DUPLICATION, 20d),
      newDoc(NEW_DUPLICATION, 50d),
      newDoc(NEW_DUPLICATION, 80d),
      newDoc(NEW_DUPLICATION, 100d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(NEW_DUPLICATION)).getFacets();

    assertThat(facets.get(NEW_DUPLICATION)).containsExactly(
      entry("NO_DATA", 2L),
      entry("*-3.0", 3L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 4L),
      entry("10.0-20.0", 2L),
      entry("20.0-*", 5L));
  }

  @Test
  @UseDataProvider("rating_metric_keys")
  public void facet_on_rating(String metricKey) {
    index(
      // 3 docs with rating A
      newDoc(metricKey, 1d),
      newDoc(metricKey, 1d),
      newDoc(metricKey, 1d),
      // 2 docs with rating B
      newDoc(metricKey, 2d),
      newDoc(metricKey, 2d),
      // 4 docs with rating C
      newDoc(metricKey, 3d),
      newDoc(metricKey, 3d),
      newDoc(metricKey, 3d),
      newDoc(metricKey, 3d),
      // 2 docs with rating D
      newDoc(metricKey, 4d),
      newDoc(metricKey, 4d),
      // 5 docs with rating E
      newDoc(metricKey, 5d),
      newDoc(metricKey, 5d),
      newDoc(metricKey, 5d),
      newDoc(metricKey, 5d),
      newDoc(metricKey, 5d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(metricKey)).getFacets();

    assertThat(facets.get(metricKey)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 2L),
      entry("5", 5L));
  }

  @Test
  @UseDataProvider("rating_metric_keys")
  public void facet_on_rating_is_sticky(String metricKey) {
    index(
      // docs with rating A
      newDoc(metricKey, 1d, NCLOC, 100d, COVERAGE, 0d),
      newDoc(metricKey, 1d, NCLOC, 200d, COVERAGE, 0d),
      newDoc(metricKey, 1d, NCLOC, 999d, COVERAGE, 0d),
      // docs with rating B
      newDoc(metricKey, 2d, NCLOC, 2000d, COVERAGE, 0d),
      newDoc(metricKey, 2d, NCLOC, 5000d, COVERAGE, 0d),
      // docs with rating C
      newDoc(metricKey, 3d, NCLOC, 20000d, COVERAGE, 0d),
      newDoc(metricKey, 3d, NCLOC, 30000d, COVERAGE, 0d),
      newDoc(metricKey, 3d, NCLOC, 40000d, COVERAGE, 0d),
      newDoc(metricKey, 3d, NCLOC, 50000d, COVERAGE, 0d),
      // docs with rating D
      newDoc(metricKey, 4d, NCLOC, 120000d, COVERAGE, 0d),
      // docs with rating E
      newDoc(metricKey, 5d, NCLOC, 600000d, COVERAGE, 40d),
      newDoc(metricKey, 5d, NCLOC, 700000d, COVERAGE, 50d),
      newDoc(metricKey, 5d, NCLOC, 800000d, COVERAGE, 60d));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .addMetricCriterion(MetricCriterion.create(metricKey, Operator.LT, 3d))
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LT, 30d)),
      new SearchOptions().addFacets(metricKey, NCLOC)).getFacets();

    // Sticky facet on maintainability rating does not take into account maintainability rating filter
    assertThat(facets.get(metricKey)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 1L),
      entry("5", 0L));
    // But facet on ncloc does well take into into filters
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 3L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 0L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  @UseDataProvider("rating_metric_keys")
  public void facet_on_rating_contains_only_projects_authorized_for_user(String metricKey) {
    // User can see these projects
    indexForUser(USER1,
      // 3 docs with rating A
      newDoc(metricKey, 1d),
      newDoc(metricKey, 1d),
      newDoc(metricKey, 1d),
      // 2 docs with rating B
      newDoc(metricKey, 2d),
      newDoc(metricKey, 2d));

    // User cannot see these projects
    indexForUser(USER2,
      // docs with rating C
      newDoc(metricKey, 3d),
      // docs with rating D
      newDoc(metricKey, 4d),
      // docs with rating E
      newDoc(metricKey, 5d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(metricKey)).getFacets();

    assertThat(facets.get(metricKey)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 0L),
      entry("4", 0L),
      entry("5", 0L));
  }

  @Test
  public void facet_quality_gate() {
    index(
      // 2 docs with QG OK
      newDoc().setQualityGateStatus(OK.name()),
      newDoc().setQualityGateStatus(OK.name()),
      // 3 docs with QG WARN
      newDoc().setQualityGateStatus(WARN.name()),
      newDoc().setQualityGateStatus(WARN.name()),
      newDoc().setQualityGateStatus(WARN.name()),
      // 4 docs with QG ERROR
      newDoc().setQualityGateStatus(ERROR.name()),
      newDoc().setQualityGateStatus(ERROR.name()),
      newDoc().setQualityGateStatus(ERROR.name()),
      newDoc().setQualityGateStatus(ERROR.name()));

    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(ALERT_STATUS_KEY)).getFacets().get(ALERT_STATUS_KEY);

    assertThat(result).containsOnly(
      entry(ERROR.name(), 4L),
      entry(WARN.name(), 3L),
      entry(OK.name(), 2L));
  }

  @Test
  public void facet_quality_gate_is_sticky() {
    index(
      // 2 docs with QG OK
      newDoc(NCLOC, 10d, COVERAGE, 0d).setQualityGateStatus(OK.name()),
      newDoc(NCLOC, 10d, COVERAGE, 0d).setQualityGateStatus(OK.name()),
      // 3 docs with QG WARN
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGateStatus(WARN.name()),
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGateStatus(WARN.name()),
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGateStatus(WARN.name()),
      // 4 docs with QG ERROR
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGateStatus(ERROR.name()),
      newDoc(NCLOC, 5000d, COVERAGE, 40d).setQualityGateStatus(ERROR.name()),
      newDoc(NCLOC, 12000d, COVERAGE, 50d).setQualityGateStatus(ERROR.name()),
      newDoc(NCLOC, 13000d, COVERAGE, 60d).setQualityGateStatus(ERROR.name()));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .setQualityGateStatus(ERROR)
      .addMetricCriterion(MetricCriterion.create(COVERAGE, Operator.LT, 55d)),
      new SearchOptions().addFacets(ALERT_STATUS_KEY, NCLOC)).getFacets();

    // Sticky facet on quality gate does not take into account quality gate filter
    assertThat(facets.get(ALERT_STATUS_KEY)).containsOnly(
      entry(OK.name(), 2L),
      entry(WARN.name(), 3L),
      entry(ERROR.name(), 3L));
    // But facet on ncloc does well take into into filters
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 1L),
      entry("1000.0-10000.0", 1L),
      entry("10000.0-100000.0", 1L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  public void facet_quality_gate_contains_only_projects_authorized_for_user() {
    // User can see these projects
    indexForUser(USER1,
      // 2 docs with QG OK
      newDoc().setQualityGateStatus(OK.name()),
      newDoc().setQualityGateStatus(OK.name()),
      // 3 docs with QG WARN
      newDoc().setQualityGateStatus(WARN.name()),
      newDoc().setQualityGateStatus(WARN.name()),
      newDoc().setQualityGateStatus(WARN.name()));

    // User cannot see these projects
    indexForUser(USER2,
      // 4 docs with QG ERROR
      newDoc().setQualityGateStatus(ERROR.name()),
      newDoc().setQualityGateStatus(ERROR.name()),
      newDoc().setQualityGateStatus(ERROR.name()),
      newDoc().setQualityGateStatus(ERROR.name()));

    userSession.logIn(USER1);
    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(ALERT_STATUS_KEY)).getFacets().get(ALERT_STATUS_KEY);

    assertThat(result).containsOnly(
      entry(ERROR.name(), 0L),
      entry(WARN.name(), 3L),
      entry(OK.name(), 2L));
  }

  @Test
  public void facet_languages() {
    index(
      newDoc().setLanguages(singletonList("java")),
      newDoc().setLanguages(singletonList("java")),
      newDoc().setLanguages(singletonList("xoo")),
      newDoc().setLanguages(singletonList("xml")),
      newDoc().setLanguages(asList("<null>", "java")),
      newDoc().setLanguages(asList("<null>", "java", "xoo")));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(LANGUAGES)).getFacets();

    assertThat(facets.get(LANGUAGES)).containsOnly(
      entry("<null>", 2L),
      entry("java", 4L),
      entry("xoo", 2L),
      entry("xml", 1L));
  }

  @Test
  public void facet_languages_is_limited_to_10_languages() {
    index(
      newDoc().setLanguages(asList("<null>", "java", "xoo", "css", "cpp")),
      newDoc().setLanguages(asList("xml", "php", "python", "perl", "ruby")),
      newDoc().setLanguages(asList("js", "scala")));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(LANGUAGES)).getFacets();

    assertThat(facets.get(LANGUAGES)).hasSize(10);
  }

  @Test
  public void facet_languages_is_sticky() {
    index(
      newDoc(NCLOC, 10d).setLanguages(singletonList("java")),
      newDoc(NCLOC, 10d).setLanguages(singletonList("java")),
      newDoc(NCLOC, 10d).setLanguages(singletonList("xoo")),
      newDoc(NCLOC, 100d).setLanguages(singletonList("xml")),
      newDoc(NCLOC, 100d).setLanguages(asList("<null>", "java")),
      newDoc(NCLOC, 5000d).setLanguages(asList("<null>", "java", "xoo")));

    Facets facets = underTest.search(
      new ProjectMeasuresQuery().setLanguages(ImmutableSet.of("java")),
      new SearchOptions().addFacets(LANGUAGES, NCLOC)).getFacets();

    // Sticky facet on language does not take into account language filter
    assertThat(facets.get(LANGUAGES)).containsOnly(
      entry("<null>", 2L),
      entry("java", 4L),
      entry("xoo", 2L),
      entry("xml", 1L));
    // But facet on ncloc does well take account into filters
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 3L),
      entry("1000.0-10000.0", 1L),
      entry("10000.0-100000.0", 0L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  public void facet_languages_returns_more_than_10_languages_when_languages_filter_contains_value_not_in_top_10() {
    index(
      newDoc().setLanguages(asList("<null>", "java", "xoo", "css", "cpp")),
      newDoc().setLanguages(asList("xml", "php", "python", "perl", "ruby")),
      newDoc().setLanguages(asList("js", "scala")));

    Facets facets = underTest.search(new ProjectMeasuresQuery().setLanguages(ImmutableSet.of("xoo", "xml")), new SearchOptions().addFacets(LANGUAGES)).getFacets();

    assertThat(facets.get(LANGUAGES)).containsOnly(
      entry("<null>", 1L),
      entry("cpp", 1L),
      entry("css", 1L),
      entry("java", 1L),
      entry("js", 1L),
      entry("perl", 1L),
      entry("php", 1L),
      entry("python", 1L),
      entry("ruby", 1L),
      entry("scala", 1L),
      entry("xoo", 1L),
      entry("xml", 1L));
  }

  @Test
  public void facet_languages_contains_only_projects_authorized_for_user() {
    // User can see these projects
    indexForUser(USER1,
      newDoc().setLanguages(singletonList("java")),
      newDoc().setLanguages(asList("java", "xoo")));

    // User cannot see these projects
    indexForUser(USER2,
      newDoc().setLanguages(singletonList("java")),
      newDoc().setLanguages(asList("java", "xoo")));

    userSession.logIn(USER1);
    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(LANGUAGES)).getFacets().get(LANGUAGES);

    assertThat(result).containsOnly(
      entry("java", 2L),
      entry("xoo", 1L));
  }

  @Test
  public void facet_tags() {
    index(
      newDoc().setTags(newArrayList("finance", "offshore", "java")),
      newDoc().setTags(newArrayList("finance", "javascript")),
      newDoc().setTags(newArrayList("marketing", "finance")),
      newDoc().setTags(newArrayList("marketing", "offshore")),
      newDoc().setTags(newArrayList("finance", "marketing")),
      newDoc().setTags(newArrayList("finance")));

    Map<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(FIELD_TAGS)).getFacets().get(FIELD_TAGS);

    assertThat(result).containsOnly(
      entry("finance", 5L),
      entry("marketing", 3L),
      entry("offshore", 2L),
      entry("java", 1L),
      entry("javascript", 1L));
  }

  @Test
  public void facet_tags_is_sticky() {
    index(
      newDoc().setTags(newArrayList("finance")).setQualityGateStatus(OK.name()),
      newDoc().setTags(newArrayList("finance")).setQualityGateStatus(ERROR.name()),
      newDoc().setTags(newArrayList("cpp")).setQualityGateStatus(WARN.name()));

    Facets facets = underTest.search(
      new ProjectMeasuresQuery().setTags(newHashSet("cpp")),
      new SearchOptions().addFacets(FIELD_TAGS).addFacets(ALERT_STATUS_KEY))
      .getFacets();

    assertThat(facets.get(FIELD_TAGS)).containsOnly(
      entry("finance", 2L),
      entry("cpp", 1L));
    assertThat(facets.get(ALERT_STATUS_KEY)).containsOnly(
      entry(OK.name(), 0L),
      entry(ERROR.name(), 0L),
      entry(WARN.name(), 1L));
  }

  @Test
  public void facet_tags_returns_10_elements_by_default() {
    index(
      newDoc().setTags(newArrayList("finance1", "finance2", "finance3", "finance4", "finance5", "finance6", "finance7", "finance8", "finance9", "finance10")),
      newDoc().setTags(newArrayList("finance1", "finance2", "finance3", "finance4", "finance5", "finance6", "finance7", "finance8", "finance9", "finance10")),
      newDoc().setTags(newArrayList("solo")));

    Map<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(FIELD_TAGS)).getFacets().get(FIELD_TAGS);

    assertThat(result).hasSize(10).containsOnlyKeys("finance1", "finance2", "finance3", "finance4", "finance5", "finance6", "finance7", "finance8", "finance9", "finance10");
  }

  @Test
  public void facet_tags_returns_more_than_10_tags_when_tags_filter_contains_value_not_in_top_10() {
    index(
      newDoc().setTags(newArrayList("finance1", "finance2", "finance3", "finance4", "finance5", "finance6", "finance7", "finance8", "finance9", "finance10")),
      newDoc().setTags(newArrayList("finance1", "finance2", "finance3", "finance4", "finance5", "finance6", "finance7", "finance8", "finance9", "finance10")),
      newDoc().setTags(newArrayList("solo", "solo2")));

    Map<String, Long> result = underTest.search(new ProjectMeasuresQuery().setTags(ImmutableSet.of("solo", "solo2")), new SearchOptions().addFacets(FIELD_TAGS)).getFacets()
      .get(FIELD_TAGS);

    assertThat(result).hasSize(12).containsOnlyKeys("finance1", "finance2", "finance3", "finance4", "finance5", "finance6", "finance7", "finance8", "finance9", "finance10", "solo",
      "solo2");
  }

  @Test
  public void search_tags() {
    index(
      newDoc().setTags(newArrayList("finance", "offshore", "java")),
      newDoc().setTags(newArrayList("official", "javascript")),
      newDoc().setTags(newArrayList("marketing", "official")),
      newDoc().setTags(newArrayList("marketing", "Madhoff")),
      newDoc().setTags(newArrayList("finance", "offshore")),
      newDoc().setTags(newArrayList("offshore")));

    List<String> result = underTest.searchTags("off", 10);

    assertThat(result).containsOnly("offshore", "official", "Madhoff");
  }

  @Test
  public void search_tags_return_all_tags() {
    index(
      newDoc().setTags(newArrayList("finance", "offshore", "java")),
      newDoc().setTags(newArrayList("official", "javascript")),
      newDoc().setTags(newArrayList("marketing", "official")),
      newDoc().setTags(newArrayList("marketing", "Madhoff")),
      newDoc().setTags(newArrayList("finance", "offshore")),
      newDoc().setTags(newArrayList("offshore")));

    List<String> result = underTest.searchTags(null, 10);

    assertThat(result).containsOnly("offshore", "official", "Madhoff", "finance", "marketing", "java", "javascript");
  }

  @Test
  public void search_tags_in_lexical_order() {
    index(
      newDoc().setTags(newArrayList("finance", "offshore", "java")),
      newDoc().setTags(newArrayList("official", "javascript")),
      newDoc().setTags(newArrayList("marketing", "official")),
      newDoc().setTags(newArrayList("marketing", "Madhoff")),
      newDoc().setTags(newArrayList("finance", "offshore")),
      newDoc().setTags(newArrayList("offshore")));

    List<String> result = underTest.searchTags(null, 10);

    assertThat(result).containsExactly("Madhoff", "finance", "java", "javascript", "marketing", "official", "offshore");
  }

  @Test
  public void search_tags_only_of_authorized_projects() {
    indexForUser(USER1,
      newDoc(PROJECT1).setTags(singletonList("finance")),
      newDoc(PROJECT2).setTags(singletonList("marketing")));
    indexForUser(USER2,
      newDoc(PROJECT3).setTags(singletonList("offshore")));

    userSession.logIn(USER1);

    List<String> result = underTest.searchTags(null, 10);

    assertThat(result).containsOnly("finance", "marketing");
  }

  @Test
  public void search_tags_with_no_tags() {
    List<String> result = underTest.searchTags("whatever", 10);

    assertThat(result).isEmpty();
  }

  @Test
  public void search_tags_with_page_size_at_0() {
    index(newDoc().setTags(newArrayList("offshore")));

    List<String> result = underTest.searchTags(null, 0);

    assertThat(result).isEmpty();
  }

  @Test
  public void search_statistics() {
    es.putDocuments(INDEX_TYPE_PROJECT_MEASURES,
      newDoc("lines", 10, "ncloc", 20, "coverage", 80)
        .setLanguages(Arrays.asList("java", "cs", "js"))
        .setNclocLanguageDistributionFromMap(ImmutableMap.of("java", 200, "cs", 250, "js", 50)),
      newDoc("lines", 20, "ncloc", 30, "coverage", 80)
        .setLanguages(Arrays.asList("java", "python", "kotlin"))
        .setNclocLanguageDistributionFromMap(ImmutableMap.of("java", 300, "python", 100, "kotlin", 404)));

    ProjectMeasuresStatistics result = underTest.searchTelemetryStatistics();

    assertThat(result.getProjectCount()).isEqualTo(2);
    assertThat(result.getLines()).isEqualTo(30);
    assertThat(result.getNcloc()).isEqualTo(50);
    assertThat(result.getProjectCountByLanguage()).containsOnly(
      entry("java", 2L), entry("cs", 1L), entry("js", 1L), entry("python", 1L), entry("kotlin", 1L));
    assertThat(result.getNclocByLanguage()).containsOnly(
      entry("java", 500L), entry("cs", 250L), entry("js", 50L), entry("python", 100L), entry("kotlin", 404L));
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size must be lower than or equals to 500");

    underTest.searchTags("whatever", 501);
  }

  private void index(ProjectMeasuresDoc... docs) {
    es.putDocuments(INDEX_TYPE_PROJECT_MEASURES, docs);
    for (ProjectMeasuresDoc doc : docs) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(doc.getId(), Qualifiers.PROJECT);
      access.allowAnyone();
      authorizationIndexerTester.allow(access);
    }
  }

  private void indexForUser(UserDto user, ProjectMeasuresDoc... docs) {
    es.putDocuments(INDEX_TYPE_PROJECT_MEASURES, docs);
    for (ProjectMeasuresDoc doc : docs) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(doc.getId(), Qualifiers.PROJECT);
      access.addUserId(user.getId());
      authorizationIndexerTester.allow(access);
    }
  }

  private void indexForGroup(GroupDto group, ProjectMeasuresDoc... docs) {
    es.putDocuments(INDEX_TYPE_PROJECT_MEASURES, docs);
    for (ProjectMeasuresDoc doc : docs) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(doc.getId(), Qualifiers.PROJECT);
      access.addGroupId(group.getId());
      authorizationIndexerTester.allow(access);
    }
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project) {
    return new ProjectMeasuresDoc()
      .setOrganizationUuid(project.getOrganizationUuid())
      .setId(project.uuid())
      .setKey(project.getDbKey())
      .setName(project.name());
  }

  private static ProjectMeasuresDoc newDoc() {
    return newDoc(ComponentTesting.newPrivateProjectDto(ORG));
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project, String metric1, Object value1) {
    return newDoc(project).setMeasures(newArrayList(newMeasure(metric1, value1)));
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project, String metric1, Object value1, String metric2, Object value2) {
    return newDoc(project).setMeasures(newArrayList(newMeasure(metric1, value1), newMeasure(metric2, value2)));
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project, String metric1, Object value1, String metric2, Object value2, String metric3, Object value3) {
    return newDoc(project).setMeasures(newArrayList(newMeasure(metric1, value1), newMeasure(metric2, value2), newMeasure(metric3, value3)));
  }

  private static Map<String, Object> newMeasure(String key, Object value) {
    return ImmutableMap.of("key", key, "value", value);
  }

  private static ProjectMeasuresDoc newDocWithNoMeasure() {
    return newDoc(ComponentTesting.newPrivateProjectDto(ORG));
  }

  private static ProjectMeasuresDoc newDoc(String metric1, Object value1) {
    return newDoc(ComponentTesting.newPrivateProjectDto(ORG), metric1, value1);
  }

  private static ProjectMeasuresDoc newDoc(String metric1, Object value1, String metric2, Object value2) {
    return newDoc(ComponentTesting.newPrivateProjectDto(ORG), metric1, value1, metric2, value2);
  }

  private static ProjectMeasuresDoc newDoc(String metric1, Object value1, String metric2, Object value2, String metric3, Object value3) {
    return newDoc(ComponentTesting.newPrivateProjectDto(ORG), metric1, value1, metric2, value2, metric3, value3);
  }

  private void assertResults(ProjectMeasuresQuery query, ComponentDto... expectedProjects) {
    List<String> result = underTest.search(query, new SearchOptions()).getIds();
    assertThat(result).containsExactly(Arrays.stream(expectedProjects).map(ComponentDto::uuid).toArray(String[]::new));
  }

  private void assertNoResults(ProjectMeasuresQuery query) {
    assertResults(query);
  }
}
