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
package org.sonar.server.measure.index;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.measure.index.ProjectMeasuresQuery.Operator;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerDao;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURE;

public class ProjectMeasuresIndexTest {

  private static final String MAINTAINABILITY_RATING = "sqale_rating";
  private static final String RELIABILITY_RATING = "reliability_rating";
  private static final String SECURITY_RATING = "security_rating";
  private static final String COVERAGE = "coverage";
  private static final String DUPLICATION = "duplicated_lines_density";
  private static final String NCLOC = "ncloc";

  private static final OrganizationDto ORG = OrganizationTesting.newOrganizationDto();
  private static final ComponentDto PROJECT1 = newProjectDto(ORG).setUuid("Project-1").setName("Project 1").setKey("key-1");
  private static final ComponentDto PROJECT2 = newProjectDto(ORG).setUuid("Project-2").setName("Project 2").setKey("key-2");
  private static final ComponentDto PROJECT3 = newProjectDto(ORG).setUuid("Project-3").setName("Project 3").setKey("key-3");
  private static final ComponentDto PROJECT4 = newProjectDto(ORG).setUuid("Project-4").setName("Project 4").setKey("key-4");
  private static final UserDto USER1 = newUserDto();
  private static final UserDto USER2 = newUserDto();
  private static final GroupDto GROUP1 = newGroupDto();
  private static final GroupDto GROUP2 = newGroupDto();

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ProjectMeasuresIndexer projectMeasureIndexer = new ProjectMeasuresIndexer(System2.INSTANCE, null, es.client());
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, projectMeasureIndexer);
  private ProjectMeasuresIndex underTest = new ProjectMeasuresIndex(es.client(), new AuthorizationTypeSupport(userSession));

  @Test
  public void return_empty_if_no_projects() {
    assertNoResults(new ProjectMeasuresQuery());
  }

  @Test
  public void default_sort_is_by_ascending_case_insensitive_name_then_by_key() {
    ComponentDto windows = newProjectDto(ORG).setUuid("windows").setName("Windows").setKey("project1");
    ComponentDto apachee = newProjectDto(ORG).setUuid("apachee").setName("apachee").setKey("project2");
    ComponentDto apache1 = newProjectDto(ORG).setUuid("apache-1").setName("Apache").setKey("project3");
    ComponentDto apache2 = newProjectDto(ORG).setUuid("apache-2").setName("Apache").setKey("project4");
    index(newDoc(windows), newDoc(apachee), newDoc(apache1), newDoc(apache2));

    assertResults(new ProjectMeasuresQuery(), apache1, apache2, apachee, windows);
  }

  @Test
  public void sort_by_insensitive_name() {
    ComponentDto windows = newProjectDto(ORG).setUuid("windows").setName("Windows");
    ComponentDto apachee = newProjectDto(ORG).setUuid("apachee").setName("apachee");
    ComponentDto apache = newProjectDto(ORG).setUuid("apache").setName("Apache");
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
    ComponentDto windows = newProjectDto(ORG).setUuid("windows").setName("Windows").setKey("project1");
    ComponentDto apachee = newProjectDto(ORG).setUuid("apachee").setName("apachee").setKey("project2");
    ComponentDto apache1 = newProjectDto(ORG).setUuid("apache-1").setName("Apache").setKey("project3");
    ComponentDto apache2 = newProjectDto(ORG).setUuid("apache-2").setName("Apache").setKey("project4");
    index(
      newDoc(windows, NCLOC, 10_000d),
      newDoc(apachee, NCLOC, 5_000d),
      newDoc(apache1, NCLOC, 5_000d),
      newDoc(apache2, NCLOC, 5_000d));

    assertResults(new ProjectMeasuresQuery().setSort("ncloc").setAsc(true), apache1, apache2, apachee, windows);
    assertResults(new ProjectMeasuresQuery().setSort("ncloc").setAsc(false), windows, apache1, apache2, apachee);
  }

  @Test
  public void paginate_results() {
    IntStream.rangeClosed(1, 9)
      .forEach(i -> index(newDoc(newProjectDto(ORG, "P" + i))));

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
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LT, 80d));

    assertResults(query, PROJECT1);
  }

  @Test
  public void filter_with_lower_than_or_equals() {
    index(
      newDoc(PROJECT1, COVERAGE, 79d, NCLOC, 10_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_000d),
      newDoc(PROJECT3, COVERAGE, 81d, NCLOC, 10_000d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LTE, 80d));

    assertResults(query, PROJECT1, PROJECT2);
  }

  @Test
  public void filter_with_greater_than() {
    index(
      newDoc(PROJECT1, COVERAGE, 80d, NCLOC, 30_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 30_001d),
      newDoc(PROJECT3, COVERAGE, 80d, NCLOC, 30_001d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 30_000d));
    assertResults(query, PROJECT2, PROJECT3);

    query = new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 100_000d));
    assertNoResults(query);
  }

  @Test
  public void filter_with_greater_than_or_equals() {
    index(
      newDoc(PROJECT1, COVERAGE, 80d, NCLOC, 30_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 30_001d),
      newDoc(PROJECT3, COVERAGE, 80d, NCLOC, 30_001d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GTE, 30_001d));
    assertResults(query, PROJECT2, PROJECT3);

    query = new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GTE, 100_000d));
    assertNoResults(query);
  }

  @Test
  public void filter_with_equals() {
    index(
      newDoc(PROJECT1, COVERAGE, 79d, NCLOC, 10_000d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_000d),
      newDoc(PROJECT3, COVERAGE, 81d, NCLOC, 10_000d));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.EQ, 80d));

    assertResults(query, PROJECT2);
  }

  @Test
  public void filter_on_several_metrics() {
    index(
      newDoc(PROJECT1, COVERAGE, 81d, NCLOC, 10_001d),
      newDoc(PROJECT2, COVERAGE, 80d, NCLOC, 10_001d),
      newDoc(PROJECT3, COVERAGE, 79d, NCLOC, 10_000d));

    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LTE, 80d))
      .addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 10_000d))
      .addMetricCriterion(new MetricCriterion(NCLOC, Operator.LT, 11_000d));
    assertResults(esQuery, PROJECT2);
  }

  @Test
  public void filter_on_quality_gate_status() {
    index(
      newDoc(PROJECT1).setQualityGate("OK"),
      newDoc(PROJECT2).setQualityGate("OK"),
      newDoc(PROJECT3).setQualityGate("WARN"));

    ProjectMeasuresQuery query = new ProjectMeasuresQuery().setQualityGateStatus(OK);
    assertResults(query, PROJECT1, PROJECT2);
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
  public void filter_on_organization() {
    OrganizationDto org1 = OrganizationTesting.newOrganizationDto();
    OrganizationDto org2 = OrganizationTesting.newOrganizationDto();
    ComponentDto projectInOrg1 = newProjectDto(org1);
    ComponentDto projectInOrg2 = newProjectDto(org2);
    index(newDoc(projectInOrg1), newDoc(projectInOrg2));

    ProjectMeasuresQuery query1 = new ProjectMeasuresQuery().setOrganizationUuid(org1.getUuid());
    assertResults(query1, projectInOrg1);

    ProjectMeasuresQuery query2 = new ProjectMeasuresQuery().setOrganizationUuid(org2.getUuid());
    assertResults(query2, projectInOrg2);

    ProjectMeasuresQuery query3 = new ProjectMeasuresQuery().setOrganizationUuid("another_org");
    assertNoResults(query3);
  }

  @Test
  public void return_only_projects_authorized_for_user() throws Exception {
    indexForUser(USER1, newDoc(PROJECT1), newDoc(PROJECT2));
    indexForUser(USER2, newDoc(PROJECT3));

    userSession.logIn(USER1);
    assertResults(new ProjectMeasuresQuery(), PROJECT1, PROJECT2);
  }

  @Test
  public void return_only_projects_authorized_for_user_groups() throws Exception {
    indexForGroup(GROUP1, newDoc(PROJECT1), newDoc(PROJECT2));
    indexForGroup(GROUP2, newDoc(PROJECT3));

    userSession.logIn().setGroups(GROUP1);
    assertResults(new ProjectMeasuresQuery(), PROJECT1, PROJECT2);
  }

  @Test
  public void return_only_projects_authorized_for_user_and_groups() throws Exception {
    indexForUser(USER1, newDoc(PROJECT1), newDoc(PROJECT2));
    indexForGroup(GROUP1, newDoc(PROJECT3));

    userSession.logIn(USER1).setGroups(GROUP1);
    assertResults(new ProjectMeasuresQuery(), PROJECT1, PROJECT2, PROJECT3);
  }

  @Test
  public void anonymous_user_can_only_access_projects_authorized_for_anyone() throws Exception {
    index(newDoc(PROJECT1));
    indexForUser(USER1, newDoc(PROJECT2));

    userSession.anonymous();
    assertResults(new ProjectMeasuresQuery(), PROJECT1);
  }

  @Test
  public void does_not_return_facet_when_no_facets_in_options() throws Exception {
    index(
      newDoc(PROJECT1, NCLOC, 10d, COVERAGE_KEY, 30d, MAINTAINABILITY_RATING, 3d)
        .setQualityGate(OK.name()));

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
      .addMetricCriterion(new MetricCriterion(NCLOC, Operator.LT, 10_000d))
      .addMetricCriterion(new MetricCriterion(DUPLICATION, Operator.LT, 10d)),
      new SearchOptions().addFacets(NCLOC, COVERAGE)).getFacets();

    // Sticky facet on ncloc does not take into account ncloc filter
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 1L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 3L),
      entry("100000.0-500000.0", 2L),
      entry("500000.0-*", 0L));
    // But facet on coverage does well take into into filters
    assertThat(facets.get(COVERAGE)).containsExactly(
      entry("*-30.0", 3L),
      entry("30.0-50.0", 0L),
      entry("50.0-70.0", 0L),
      entry("70.0-80.0", 0L),
      entry("80.0-*", 0L));
  }

  @Test
  public void facet_ncloc_contains_only_projects_authorized_for_user() throws Exception {
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
  public void facet_coverage() {
    index(
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

    assertThat(facets.get(COVERAGE)).containsExactly(
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 4L),
      entry("70.0-80.0", 2L),
      entry("80.0-*", 5L));
  }

  @Test
  public void facet_coverage_is_sticky() {
    index(
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
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LT, 30d))
      .addMetricCriterion(new MetricCriterion(DUPLICATION, Operator.LT, 10d)),
      new SearchOptions().addFacets(COVERAGE, NCLOC)).getFacets();

    // Sticky facet on coverage does not take into account coverage filter
    assertThat(facets.get(COVERAGE)).containsExactly(
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
  public void facet_coverage_contains_only_projects_authorized_for_user() throws Exception {
    // User can see these projects
    indexForUser(USER1,
      // docs with coverage<30%
      newDoc(COVERAGE, 0d),
      newDoc(COVERAGE, 0d),
      newDoc(COVERAGE, 29d),
      // docs with coverage>=30% and coverage<50%
      newDoc(COVERAGE, 30d),
      newDoc(COVERAGE, 49d));

    // User cannot see these projects
    indexForUser(USER2,
      // docs with coverage>=50% and coverage<70%
      newDoc(COVERAGE, 50d),
      // docs with coverage>=70% and coverage<80%
      newDoc(COVERAGE, 70d),
      // docs with coverage>= 80%
      newDoc(COVERAGE, 80d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(COVERAGE)).getFacets();

    assertThat(facets.get(COVERAGE)).containsExactly(
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 0L),
      entry("70.0-80.0", 0L),
      entry("80.0-*", 0L));
  }

  @Test
  public void facet_duplicated_lines_density() {
    index(
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

    assertThat(facets.get(DUPLICATION)).containsExactly(
      entry("*-3.0", 3L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 4L),
      entry("10.0-20.0", 2L),
      entry("20.0-*", 5L));
  }

  @Test
  public void facet_duplicated_lines_density_is_sticky() {
    index(
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
      .addMetricCriterion(new MetricCriterion(DUPLICATION, Operator.LT, 10d))
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LT, 30d)),
      new SearchOptions().addFacets(DUPLICATION, NCLOC)).getFacets();

    // Sticky facet on duplication does not take into account duplication filter
    assertThat(facets.get(DUPLICATION)).containsExactly(
      entry("*-3.0", 1L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 1L),
      entry("10.0-20.0", 2L),
      entry("20.0-*", 0L));
    // But facet on ncloc does well take into into filters
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 1L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 1L),
      entry("100000.0-500000.0", 0L),
      entry("500000.0-*", 0L));
  }

  @Test
  public void facet_duplicated_lines_density_contains_only_projects_authorized_for_user() throws Exception {
    // User can see these projects
    indexForUser(USER1,
      // docs with duplication<3%
      newDoc(DUPLICATION, 0d),
      newDoc(DUPLICATION, 0d),
      newDoc(DUPLICATION, 2.9d),
      // docs with duplication>=3% and duplication<5%
      newDoc(DUPLICATION, 3d),
      newDoc(DUPLICATION, 4.9d));

    // User cannot see these projects
    indexForUser(USER2,
      // docs with duplication>=5% and duplication<10%
      newDoc(DUPLICATION, 5d),
      // docs with duplication>=10% and duplication<20%
      newDoc(DUPLICATION, 10d),
      // docs with duplication>= 20%
      newDoc(DUPLICATION, 20d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(DUPLICATION)).getFacets();

    assertThat(facets.get(DUPLICATION)).containsExactly(
      entry("*-3.0", 3L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 0L),
      entry("10.0-20.0", 0L),
      entry("20.0-*", 0L));
  }

  @Test
  public void facet_maintainability_rating() {
    index(
      // 3 docs with rating A
      newDoc(MAINTAINABILITY_RATING, 1d),
      newDoc(MAINTAINABILITY_RATING, 1d),
      newDoc(MAINTAINABILITY_RATING, 1d),
      // 2 docs with rating B
      newDoc(MAINTAINABILITY_RATING, 2d),
      newDoc(MAINTAINABILITY_RATING, 2d),
      // 4 docs with rating C
      newDoc(MAINTAINABILITY_RATING, 3d),
      newDoc(MAINTAINABILITY_RATING, 3d),
      newDoc(MAINTAINABILITY_RATING, 3d),
      newDoc(MAINTAINABILITY_RATING, 3d),
      // 2 docs with rating D
      newDoc(MAINTAINABILITY_RATING, 4d),
      newDoc(MAINTAINABILITY_RATING, 4d),
      // 5 docs with rating E
      newDoc(MAINTAINABILITY_RATING, 5d),
      newDoc(MAINTAINABILITY_RATING, 5d),
      newDoc(MAINTAINABILITY_RATING, 5d),
      newDoc(MAINTAINABILITY_RATING, 5d),
      newDoc(MAINTAINABILITY_RATING, 5d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(MAINTAINABILITY_RATING)).getFacets();

    assertThat(facets.get(MAINTAINABILITY_RATING)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 2L),
      entry("5", 5L));
  }

  @Test
  public void facet_maintainability_rating_is_sticky() {
    index(
      // docs with rating A
      newDoc(MAINTAINABILITY_RATING, 1d, NCLOC, 100d, COVERAGE, 0d),
      newDoc(MAINTAINABILITY_RATING, 1d, NCLOC, 200d, COVERAGE, 0d),
      newDoc(MAINTAINABILITY_RATING, 1d, NCLOC, 999d, COVERAGE, 0d),
      // docs with rating B
      newDoc(MAINTAINABILITY_RATING, 2d, NCLOC, 2000d, COVERAGE, 0d),
      newDoc(MAINTAINABILITY_RATING, 2d, NCLOC, 5000d, COVERAGE, 0d),
      // docs with rating C
      newDoc(MAINTAINABILITY_RATING, 3d, NCLOC, 20000d, COVERAGE, 0d),
      newDoc(MAINTAINABILITY_RATING, 3d, NCLOC, 30000d, COVERAGE, 0d),
      newDoc(MAINTAINABILITY_RATING, 3d, NCLOC, 40000d, COVERAGE, 0d),
      newDoc(MAINTAINABILITY_RATING, 3d, NCLOC, 50000d, COVERAGE, 0d),
      // docs with rating D
      newDoc(MAINTAINABILITY_RATING, 4d, NCLOC, 120000d, COVERAGE, 0d),
      // docs with rating E
      newDoc(MAINTAINABILITY_RATING, 5d, NCLOC, 600000d, COVERAGE, 40d),
      newDoc(MAINTAINABILITY_RATING, 5d, NCLOC, 700000d, COVERAGE, 50d),
      newDoc(MAINTAINABILITY_RATING, 5d, NCLOC, 800000d, COVERAGE, 60d));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(MAINTAINABILITY_RATING, Operator.LT, 3d))
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LT, 30d)),
      new SearchOptions().addFacets(MAINTAINABILITY_RATING, NCLOC)).getFacets();

    // Sticky facet on maintainability rating does not take into account maintainability rating filter
    assertThat(facets.get(MAINTAINABILITY_RATING)).containsExactly(
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
  public void facet_maintainability_rating_contains_only_projects_authorized_for_user() throws Exception {
    // User can see these projects
    indexForUser(USER1,
      // 3 docs with rating A
      newDoc(MAINTAINABILITY_RATING, 1d),
      newDoc(MAINTAINABILITY_RATING, 1d),
      newDoc(MAINTAINABILITY_RATING, 1d),
      // 2 docs with rating B
      newDoc(MAINTAINABILITY_RATING, 2d),
      newDoc(MAINTAINABILITY_RATING, 2d));

    // User cannot see these projects
    indexForUser(USER2,
      // docs with rating C
      newDoc(MAINTAINABILITY_RATING, 3d),
      // docs with rating D
      newDoc(MAINTAINABILITY_RATING, 4d),
      // docs with rating E
      newDoc(MAINTAINABILITY_RATING, 5d));

    userSession.logIn(USER1);
    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(MAINTAINABILITY_RATING)).getFacets();

    assertThat(facets.get(MAINTAINABILITY_RATING)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 0L),
      entry("4", 0L),
      entry("5", 0L));
  }

  @Test
  public void facet_reliability_rating() {
    index(
      // 3 docs with rating A
      newDoc(RELIABILITY_RATING, 1d),
      newDoc(RELIABILITY_RATING, 1d),
      newDoc(RELIABILITY_RATING, 1d),
      // 2 docs with rating B
      newDoc(RELIABILITY_RATING, 2d),
      newDoc(RELIABILITY_RATING, 2d),
      // 4 docs with rating C
      newDoc(RELIABILITY_RATING, 3d),
      newDoc(RELIABILITY_RATING, 3d),
      newDoc(RELIABILITY_RATING, 3d),
      newDoc(RELIABILITY_RATING, 3d),
      // 2 docs with rating D
      newDoc(RELIABILITY_RATING, 4d),
      newDoc(RELIABILITY_RATING, 4d),
      // 5 docs with rating E
      newDoc(RELIABILITY_RATING, 5d),
      newDoc(RELIABILITY_RATING, 5d),
      newDoc(RELIABILITY_RATING, 5d),
      newDoc(RELIABILITY_RATING, 5d),
      newDoc(RELIABILITY_RATING, 5d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(RELIABILITY_RATING)).getFacets();

    assertThat(facets.get(RELIABILITY_RATING)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 2L),
      entry("5", 5L));
  }

  @Test
  public void facet_security_rating() {
    index(
      // 3 docs with rating A
      newDoc(SECURITY_RATING, 1.0d),
      newDoc(SECURITY_RATING, 1.0d),
      newDoc(SECURITY_RATING, 1.0d),
      // 2 docs with rating B
      newDoc(SECURITY_RATING, 2.0d),
      newDoc(SECURITY_RATING, 2.0d),
      // 4 docs with rating C
      newDoc(SECURITY_RATING, 3.0d),
      newDoc(SECURITY_RATING, 3.0d),
      newDoc(SECURITY_RATING, 3.0d),
      newDoc(SECURITY_RATING, 3.0d),
      // 2 docs with rating D
      newDoc(SECURITY_RATING, 4.0d),
      newDoc(SECURITY_RATING, 4.0d),
      // 5 docs with rating E
      newDoc(SECURITY_RATING, 5.0d),
      newDoc(SECURITY_RATING, 5.0d),
      newDoc(SECURITY_RATING, 5.0d),
      newDoc(SECURITY_RATING, 5.0d),
      newDoc(SECURITY_RATING, 5.0d));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(SECURITY_RATING)).getFacets();

    assertThat(facets.get(SECURITY_RATING)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 2L),
      entry("5", 5L));
  }

  @Test
  public void facet_quality_gate() {
    index(
      // 2 docs with QG OK
      newDoc().setQualityGate(OK.name()),
      newDoc().setQualityGate(OK.name()),
      // 3 docs with QG WARN
      newDoc().setQualityGate(WARN.name()),
      newDoc().setQualityGate(WARN.name()),
      newDoc().setQualityGate(WARN.name()),
      // 4 docs with QG ERROR
      newDoc().setQualityGate(ERROR.name()),
      newDoc().setQualityGate(ERROR.name()),
      newDoc().setQualityGate(ERROR.name()),
      newDoc().setQualityGate(ERROR.name()));

    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(ALERT_STATUS_KEY)).getFacets().get(ALERT_STATUS_KEY);

    assertThat(result).containsExactly(
      entry(ERROR.name(), 4L),
      entry(WARN.name(), 3L),
      entry(OK.name(), 2L));
  }

  @Test
  public void facet_quality_gate_is_sticky() {
    index(
      // 2 docs with QG OK
      newDoc(NCLOC, 10d, COVERAGE, 0d).setQualityGate(OK.name()),
      newDoc(NCLOC, 10d, COVERAGE, 0d).setQualityGate(OK.name()),
      // 3 docs with QG WARN
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGate(WARN.name()),
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGate(WARN.name()),
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGate(WARN.name()),
      // 4 docs with QG ERROR
      newDoc(NCLOC, 100d, COVERAGE, 0d).setQualityGate(ERROR.name()),
      newDoc(NCLOC, 5000d, COVERAGE, 40d).setQualityGate(ERROR.name()),
      newDoc(NCLOC, 12000d, COVERAGE, 50d).setQualityGate(ERROR.name()),
      newDoc(NCLOC, 13000d, COVERAGE, 60d).setQualityGate(ERROR.name()));

    Facets facets = underTest.search(new ProjectMeasuresQuery()
      .setQualityGateStatus(ERROR)
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LT, 55d)),
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
  public void facet_quality_gate_contains_only_projects_authorized_for_user() throws Exception {
    // User can see these projects
    indexForUser(USER1,
      // 2 docs with QG OK
      newDoc().setQualityGate(OK.name()),
      newDoc().setQualityGate(OK.name()),
      // 3 docs with QG WARN
      newDoc().setQualityGate(WARN.name()),
      newDoc().setQualityGate(WARN.name()),
      newDoc().setQualityGate(WARN.name()));

    // User cannot see these projects
    indexForUser(USER2,
      // 4 docs with QG ERROR
      newDoc().setQualityGate(ERROR.name()),
      newDoc().setQualityGate(ERROR.name()),
      newDoc().setQualityGate(ERROR.name()),
      newDoc().setQualityGate(ERROR.name()));

    userSession.logIn(USER1);
    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(ALERT_STATUS_KEY)).getFacets().get(ALERT_STATUS_KEY);

    assertThat(result).containsExactly(
      entry(ERROR.name(), 0L),
      entry(WARN.name(), 3L),
      entry(OK.name(), 2L));
  }

  private void index(ProjectMeasuresDoc... docs) {
    es.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, docs);
    for (ProjectMeasuresDoc doc : docs) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(doc.getId(), System.currentTimeMillis(), Qualifiers.PROJECT);
      access.allowAnyone();
      authorizationIndexerTester.allow(access);
    }
  }

  private void indexForUser(UserDto user, ProjectMeasuresDoc... docs) {
    es.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, docs);
    for (ProjectMeasuresDoc doc : docs) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(doc.getId(), System.currentTimeMillis(), Qualifiers.PROJECT);
      access.addUserId(user.getId());
      authorizationIndexerTester.allow(access);
    }
  }

  private void indexForGroup(GroupDto group, ProjectMeasuresDoc... docs) {
    es.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, docs);
    for (ProjectMeasuresDoc doc : docs) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(doc.getId(), System.currentTimeMillis(), Qualifiers.PROJECT);
      access.addGroupId(group.getId());
      authorizationIndexerTester.allow(access);
    }
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project) {
    return new ProjectMeasuresDoc()
      .setOrganizationUuid(project.getOrganizationUuid())
      .setId(project.uuid())
      .setKey(project.key())
      .setName(project.name());
  }

  private static ProjectMeasuresDoc newDoc() {
    return newDoc(newProjectDto(ORG));
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

  private static ProjectMeasuresDoc newDoc(String metric1, Object value1) {
    return newDoc(newProjectDto(ORG), metric1, value1);
  }

  private static ProjectMeasuresDoc newDoc(String metric1, Object value1, String metric2, Object value2) {
    return newDoc(newProjectDto(ORG), metric1, value1, metric2, value2);
  }

  private static ProjectMeasuresDoc newDoc(String metric1, Object value1, String metric2, Object value2, String metric3, Object value3) {
    return newDoc(newProjectDto(ORG), metric1, value1, metric2, value2, metric3, value3);
  }

  private void assertResults(ProjectMeasuresQuery query, ComponentDto... expectedProjects) {
    List<String> result = underTest.search(query, new SearchOptions()).getIds();
    assertThat(result).containsExactly(Arrays.stream(expectedProjects).map(ComponentDto::uuid).toArray(String[]::new));
  }

  private void assertNoResults(ProjectMeasuresQuery query) {
    assertResults(query);
  }
}
