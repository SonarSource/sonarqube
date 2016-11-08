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
package org.sonar.server.component.es;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.component.es.ProjectMeasuresQuery.Operator;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexTest {

  private static final String MAINTAINABILITY_RATING = "sqale_rating";
  private static final String RELIABILITY_RATING = "reliability_rating";
  private static final String SECURITY_RATING = "security_rating";
  private static final String COVERAGE = "coverage";
  private static final String DUPLICATION = "duplicated_lines_density";
  private static final String NCLOC = "ncloc";

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es);

  private ProjectMeasuresIndex underTest = new ProjectMeasuresIndex(es.client(), userSession);

  @Test
  public void empty_search() {
    List<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getIds();

    assertThat(result).isEmpty();
  }

  @Test
  public void search_sort_by_name_case_insensitive() {
    addDocs(newDoc("P1", "K1", "Windows"),
      newDoc("P3", "K3", "apachee"),
      newDoc("P2", "K2", "Apache"));

    List<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getIds();

    assertThat(result).containsExactly("P2", "P3", "P1");
  }

  @Test
  public void search_paginate_results() {
    IntStream.rangeClosed(1, 9)
      .forEach(i -> addDocs(newDoc("P" + i, "K" + i, "P" + i)));

    SearchIdResult<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().setPage(2, 3));

    assertThat(result.getIds()).containsExactly("P4", "P5", "P6");
    assertThat(result.getTotal()).isEqualTo(9);
  }

  @Test
  public void filter_with_lower_than() {
    addDocs(
      newDoc("P1", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 79d), newMeasure(NCLOC, 10_000d))),
      newDoc("P2", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d))),
      newDoc("P3", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 81d), newMeasure(NCLOC, 10_000d))));

    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LT, 80d));
    List<String> result = underTest.search(esQuery, new SearchOptions()).getIds();

    assertThat(result).containsExactly("P1");
  }

  @Test
  public void filter_with_lower_than_or_equals() {
    addDocs(
      newDoc("P1", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 79d), newMeasure(NCLOC, 10_000d))),
      newDoc("P2", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d))),
      newDoc("P3", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 81d), newMeasure(NCLOC, 10_000d))));

    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LTE, 80d));
    List<String> result = underTest.search(esQuery, new SearchOptions()).getIds();

    assertThat(result).containsExactly("P1", "P2");
  }

  @Test
  public void filter_with_greater_than() {
    addDocs(
      newDoc("P1", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 30_000d))),
      newDoc("P2", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 30_001d))),
      newDoc("P3", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 30_001d))));

    assertThat(underTest.search(new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 30_000d)),
      new SearchOptions()).getIds()).containsExactly("P2", "P3");
    assertThat(underTest.search(new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 100_000d)),
      new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void filter_with_greater_than_or_equals() {
    addDocs(
      newDoc("P1", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 30_000d))),
      newDoc("P2", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 30_001d))),
      newDoc("P3", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 30_001d))));

    assertThat(underTest.search(new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GTE, 30_001d)),
      new SearchOptions()).getIds()).containsExactly("P2", "P3");
    assertThat(underTest.search(new ProjectMeasuresQuery().addMetricCriterion(new MetricCriterion(NCLOC, Operator.GTE, 100_000d)),
      new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void filter_with_equals() {
    addDocs(
      newDoc("P1", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 79d), newMeasure(NCLOC, 10_000d))),
      newDoc("P2", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_000d))),
      newDoc("P3", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 81d), newMeasure(NCLOC, 10_000d))));

    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.EQ, 80d));
    List<String> result = underTest.search(esQuery, new SearchOptions()).getIds();

    assertThat(result).containsExactly("P2");
  }

  @Test
  public void filter_on_several_metrics() {
    addDocs(
      newDoc("P1", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 81d), newMeasure(NCLOC, 10_001d))),
      newDoc("P2", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 80d), newMeasure(NCLOC, 10_001d))),
      newDoc("P3", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 79d), newMeasure(NCLOC, 10_000d))));

    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery()
      .addMetricCriterion(new MetricCriterion(COVERAGE, Operator.LTE, 80d))
      .addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 10_000d))
      .addMetricCriterion(new MetricCriterion(NCLOC, Operator.LT, 11_000d));
    List<String> result = underTest.search(esQuery, new SearchOptions()).getIds();

    assertThat(result).containsExactly("P2");
  }

  @Test
  public void filter_on_quality_gate_status() {
    addDocs(
      newDoc("P1", "K1", "N1").setQualityGate("OK"),
      newDoc("P2", "K2", "N2").setQualityGate("OK"),
      newDoc("P3", "K3", "N3").setQualityGate("WARN"));
    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery().setQualityGateStatus(OK);

    List<String> result = underTest.search(esQuery, new SearchOptions()).getIds();

    assertThat(result).containsExactly("P1", "P2");
  }

  @Test
  public void filter_on_ids() {
    addDocs(
      newDoc("P1", "K1", "N1"),
      newDoc("P2", "K2", "N2"),
      newDoc("P3", "K3", "N3"));
    ProjectMeasuresQuery esQuery = new ProjectMeasuresQuery().setProjectUuids(newHashSet("P1", "P3"));

    List<String> result = underTest.search(esQuery, new SearchOptions()).getIds();

    assertThat(result).containsExactly("P1", "P3");
  }

  @Test
  public void return_only_projects_authorized_for_user() throws Exception {
    userSession.login("john").setUserId(10);
    addDocs(10L, null, newDoc("P1", "K1", "Windows"));
    addDocs(10L, "dev", newDoc("P2", "K2", "apachee"));
    addDocs(33L, null, newDoc("P10", "K10", "N10"));

    List<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getIds();

    assertThat(result).containsOnly("P1", "P2");
  }

  @Test
  public void return_only_projects_authorized_for_user_groups() throws Exception {
    userSession.setUserGroups("dev");
    addDocs(10L, "dev", newDoc("P1", "K1", "apachee"));
    addDocs(null, ANYONE, newDoc("P2", "K2", "N2"));
    addDocs(null, "admin", newDoc("P10", "K10", "N10"));

    List<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getIds();

    assertThat(result).containsOnly("P1", "P2");
  }

  @Test
  public void return_only_projects_authorized_for_user_and_groups() throws Exception {
    userSession.login("john").setUserId(10).setUserGroups("dev");
    addDocs(10L, null, newDoc("P1", "K1", "Windows"));
    addDocs(null, "dev", newDoc("P2", "K2", "Apache"));
    addDocs(10L, "dev", newDoc("P3", "K3", "apachee"));
    // Current user is not able to see following projects
    addDocs(null, "another group", newDoc("P5", "K5", "N5"));
    addDocs(33L, null, newDoc("P6", "K6", "N6"));
    addDocs((Long) null, null, newDoc("P7", "K7", "N7"));

    List<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getIds();

    assertThat(result).containsOnly("P1", "P2", "P3");
  }

  @Test
  public void anyone_user_can_only_access_projects_authorized_for_anyone() throws Exception {
    userSession.anonymous();
    addDocs(null, ANYONE, newDoc("P1", "K1", "N1"));
    addDocs(10L, null, newDoc("P2", "K2", "Windows"));
    addDocs(null, "admin", newDoc("P3", "K3", "N3"));

    List<String> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getIds();

    assertThat(result).containsOnly("P1");
  }

  @Test
  public void does_not_return_facet_when_no_facets_in_options() throws Exception {
    addDocs(
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 10d), newMeasure(COVERAGE_KEY, 30d), newMeasure(MAINTAINABILITY_RATING, 3d)))
        .setQualityGate(OK.name()));

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.getAll()).isEmpty();
  }

  @Test
  public void facet_ncloc() {
    addDocs(
      // 3 docs with ncloc<1K
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 999d))),
      // 2 docs with ncloc>=1K and ncloc<10K
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 1_000d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 9_999d))),
      // 4 docs with ncloc>=10K and ncloc<100K
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 10_000d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 10_000d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 11_000d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 99_000d))),
      // 2 docs with ncloc>=100K and ncloc<500K
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 100_000d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 499_000d))),
      // 5 docs with ncloc>= 500K
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 500_000d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 100_000_000d))),
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 500_000d))),
      newDoc("P54", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 1_000_000d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 100_000_000_000d))));

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
    addDocs(
      // 1 docs with ncloc<1K
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 999d), newMeasure(COVERAGE, 0d), newMeasure(DUPLICATION, 0d))),
      // 2 docs with ncloc>=1K and ncloc<10K
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 1_000d), newMeasure(COVERAGE, 10d), newMeasure(DUPLICATION, 0d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 9_999d), newMeasure(COVERAGE, 20d), newMeasure(DUPLICATION, 0d))),
      // 3 docs with ncloc>=10K and ncloc<100K
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 10_000d), newMeasure(COVERAGE, 31d), newMeasure(DUPLICATION, 0d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 11_000d), newMeasure(COVERAGE, 40d), newMeasure(DUPLICATION, 0d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 99_000d), newMeasure(COVERAGE, 50d), newMeasure(DUPLICATION, 0d))),
      // 2 docs with ncloc>=100K and ncloc<500K
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 100_000d), newMeasure(COVERAGE, 71d), newMeasure(DUPLICATION, 0d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 499_000d), newMeasure(COVERAGE, 80d), newMeasure(DUPLICATION, 0d))),
      // 1 docs with ncloc>= 500K
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 501_000d), newMeasure(COVERAGE, 81d), newMeasure(DUPLICATION, 20d))));

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
    userSession.login("john").setUserId(10);

    // User can see these projects
    addDocs(10L, null,
      // docs with ncloc<1K
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 100d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 999d))),
      // docs with ncloc>=1K and ncloc<10K
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 1_000d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 9_999d))));

    // User cannot see these projects
    addDocs(33L, null,
      // doc with ncloc>=10K and ncloc<100K
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 11_000d))),
      // doc with ncloc>=100K and ncloc<500K
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 499_000d))),
      // doc with ncloc>= 500K
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 501_000d))));

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
    addDocs(
      // 3 docs with coverage<30%
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 29d))),
      // 2 docs with coverage>=30% and coverage<50%
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 30d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 49d))),
      // 4 docs with coverage>=50% and coverage<70%
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 50d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 60d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 60d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 69d))),
      // 2 docs with coverage>=70% and coverage<80%
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 70d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 79d))),
      // 5 docs with coverage>= 80%
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 80d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 80d))),
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 90d))),
      newDoc("P54", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 90.5d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 100d))));

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
    addDocs(
      // docs with coverage<30%
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(NCLOC, 999d), newMeasure(COVERAGE, 0d), newMeasure(DUPLICATION, 0d))),
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 1_000d), newMeasure(COVERAGE, 10d), newMeasure(DUPLICATION, 0d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(NCLOC, 9_999d), newMeasure(COVERAGE, 20d), newMeasure(DUPLICATION, 0d))),
      // docs with coverage>=30% and coverage<50%
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 10_000d), newMeasure(COVERAGE, 31d), newMeasure(DUPLICATION, 0d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 11_000d), newMeasure(COVERAGE, 40d), newMeasure(DUPLICATION, 0d))),
      // docs with coverage>=50% and coverage<70%
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 99_000d), newMeasure(COVERAGE, 50d), newMeasure(DUPLICATION, 0d))),
      // docs with coverage>=70% and coverage<80%
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 100_000d), newMeasure(COVERAGE, 71d), newMeasure(DUPLICATION, 0d))),
      // docs with coverage>= 80%
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 499_000d), newMeasure(COVERAGE, 80d), newMeasure(DUPLICATION, 15d))),
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(NCLOC, 501_000d), newMeasure(COVERAGE, 810d), newMeasure(DUPLICATION, 20d))));

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
    userSession.login("john").setUserId(10);

    // User can see these projects
    addDocs(10L, null,
      // docs with coverage<30%
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(COVERAGE, 29d))),
      // docs with coverage>=30% and coverage<50%
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 30d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(COVERAGE, 49d))));

    // User cannot see these projects
    addDocs(33L, null,
      // docs with coverage>=50% and coverage<70%
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 50d))),
      // docs with coverage>=70% and coverage<80%
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 70d))),
      // docs with coverage>= 80%
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(COVERAGE, 80d))));

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
    addDocs(
      // 3 docs with duplication<3%
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 2.9d))),
      // 2 docs with duplication>=3% and duplication<5%
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(DUPLICATION, 3d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(DUPLICATION, 4.9d))),
      // 4 docs with duplication>=5% and duplication<10%
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 5d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 6d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 6d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 9.9d))),
      // 2 docs with duplication>=10% and duplication<20%
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 10d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 19.9d))),
      // 5 docs with duplication>= 20%
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 20d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 20d))),
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 50d))),
      newDoc("P54", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 80d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 100d))));

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
    addDocs(
      // docs with duplication<3%
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 0d), newMeasure(NCLOC, 999d), newMeasure(COVERAGE, 0d))),
      // docs with duplication>=3% and duplication<5%
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(DUPLICATION, 3d), newMeasure(NCLOC, 5000d), newMeasure(COVERAGE, 0d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(DUPLICATION, 4.9d), newMeasure(NCLOC, 6000d), newMeasure(COVERAGE, 0d))),
      // docs with duplication>=5% and duplication<10%
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 5d), newMeasure(NCLOC, 11000d), newMeasure(COVERAGE, 0d))),
      // docs with duplication>=10% and duplication<20%
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 10d), newMeasure(NCLOC, 120000d), newMeasure(COVERAGE, 10d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 19.9d), newMeasure(NCLOC, 130000d), newMeasure(COVERAGE, 20d))),
      // docs with duplication>= 20%
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 20d), newMeasure(NCLOC, 1000000d), newMeasure(COVERAGE, 40d))));

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
    userSession.login("john").setUserId(10);

    // User can see these projects
    addDocs(10L, null,
      // docs with duplication<3%
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(DUPLICATION, 2.9d))),
      // docs with duplication>=3% and duplication<5%
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(DUPLICATION, 3d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(DUPLICATION, 4.9d))));

    // User cannot see these projects
    addDocs(33L, null,
      // docs with duplication>=5% and duplication<10%
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 5d))),
      // docs with duplication>=10% and duplication<20%
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 10d))),
      // docs with duplication>= 20%
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(DUPLICATION, 20d))));

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
    addDocs(
      // 3 docs with rating A
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d))),
      // 2 docs with rating B
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 2d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 2d))),
      // 4 docs with rating C
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d))),
      // 2 docs with rating D
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 4d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 4d))),
      // 5 docs with rating E
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d))),
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d))),
      newDoc("P54", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d))));

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
    addDocs(
      // docs with rating A
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d), newMeasure(NCLOC, 100d), newMeasure(COVERAGE, 0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d), newMeasure(NCLOC, 200d), newMeasure(COVERAGE, 0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d), newMeasure(NCLOC, 999d), newMeasure(COVERAGE, 0d))),
      // docs with rating B
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 2d), newMeasure(NCLOC, 2000d), newMeasure(COVERAGE, 0d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 2d), newMeasure(NCLOC, 5000d), newMeasure(COVERAGE, 0d))),
      // docs with rating C
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d), newMeasure(NCLOC, 20000d), newMeasure(COVERAGE, 0d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d), newMeasure(NCLOC, 30000d), newMeasure(COVERAGE, 0d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d), newMeasure(NCLOC, 40000d), newMeasure(COVERAGE, 0d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d), newMeasure(NCLOC, 50000d), newMeasure(COVERAGE, 0d))),
      // docs with rating D
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 4d), newMeasure(NCLOC, 120000d), newMeasure(COVERAGE, 0d))),
      // docs with rating E
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d), newMeasure(NCLOC, 600000d), newMeasure(COVERAGE, 40d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d), newMeasure(NCLOC, 700000d), newMeasure(COVERAGE, 50d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d), newMeasure(NCLOC, 800000d), newMeasure(COVERAGE, 60d))));

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
    userSession.login("john").setUserId(10);

    // User can see these projects
    addDocs(10L, null,
      // 3 docs with rating A
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 1d))),
      // 2 docs with rating B
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 2d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 2d))));

    // User cannot see these projects
    addDocs(33L, null,
      // docs with rating C
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 3d))),
      // docs with rating D
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 4d))),
      // docs with rating E
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(MAINTAINABILITY_RATING, 5d))));

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
    addDocs(
      // 3 docs with rating A
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 1d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 1d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 1d))),
      // 2 docs with rating B
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 2d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 2d))),
      // 4 docs with rating C
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 3d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 3d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 3d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 3d))),
      // 2 docs with rating D
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 4d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 4d))),
      // 5 docs with rating E
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 5d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 5d))),
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 5d))),
      newDoc("P54", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 5d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(RELIABILITY_RATING, 5d))));

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
    addDocs(
      // 3 docs with rating A
      newDoc("P11", "K1", "N1").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 1.0d))),
      newDoc("P12", "K1", "N1").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 1.0d))),
      newDoc("P13", "K1", "N1").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 1.0d))),
      // 2 docs with rating B
      newDoc("P21", "K2", "N2").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 2.0d))),
      newDoc("P22", "K2", "N2").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 2.0d))),
      // 4 docs with rating C
      newDoc("P31", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 3.0d))),
      newDoc("P32", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 3.0d))),
      newDoc("P33", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 3.0d))),
      newDoc("P34", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 3.0d))),
      // 2 docs with rating D
      newDoc("P41", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 4.0d))),
      newDoc("P42", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 4.0d))),
      // 5 docs with rating E
      newDoc("P51", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 5.0d))),
      newDoc("P52", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 5.0d))),
      newDoc("P53", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 5.0d))),
      newDoc("P54", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 5.0d))),
      newDoc("P55", "K3", "N3").setMeasures(newArrayList(newMeasure(SECURITY_RATING, 5.0d))));

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
    addDocs(
      // 2 docs with QG OK
      newDoc("P11", "K1", "N1").setQualityGate(OK.name()),
      newDoc("P12", "K1", "N1").setQualityGate(OK.name()),
      // 3 docs with QG WARN
      newDoc("P21", "K1", "N1").setQualityGate(WARN.name()),
      newDoc("P22", "K1", "N1").setQualityGate(WARN.name()),
      newDoc("P23", "K1", "N1").setQualityGate(WARN.name()),
      // 4 docs with QG ERROR
      newDoc("P31", "K1", "N1").setQualityGate(ERROR.name()),
      newDoc("P32", "K1", "N1").setQualityGate(ERROR.name()),
      newDoc("P33", "K1", "N1").setQualityGate(ERROR.name()),
      newDoc("P34", "K1", "N1").setQualityGate(ERROR.name()));

    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(ALERT_STATUS_KEY)).getFacets().get(ALERT_STATUS_KEY);

    assertThat(result).containsExactly(
      entry(ERROR.name(), 4L),
      entry(WARN.name(), 3L),
      entry(OK.name(), 2L));
  }

  @Test
  public void facet_quality_gate_is_sticky() {
    addDocs(
      // 2 docs with QG OK
      newDoc("P11", "K1", "N1").setQualityGate(OK.name()).setMeasures(newArrayList(newMeasure(NCLOC, 10d), newMeasure(COVERAGE, 0d))),
      newDoc("P12", "K1", "N1").setQualityGate(OK.name()).setMeasures(newArrayList(newMeasure(NCLOC, 10d), newMeasure(COVERAGE, 0d))),
      // 3 docs with QG WARN
      newDoc("P21", "K1", "N1").setQualityGate(WARN.name()).setMeasures(newArrayList(newMeasure(NCLOC, 100d), newMeasure(COVERAGE, 0d))),
      newDoc("P22", "K1", "N1").setQualityGate(WARN.name()).setMeasures(newArrayList(newMeasure(NCLOC, 100d), newMeasure(COVERAGE, 0d))),
      newDoc("P23", "K1", "N1").setQualityGate(WARN.name()).setMeasures(newArrayList(newMeasure(NCLOC, 100d), newMeasure(COVERAGE, 0d))),
      // 4 docs with QG ERROR
      newDoc("P31", "K1", "N1").setQualityGate(ERROR.name()).setMeasures(newArrayList(newMeasure(NCLOC, 100d), newMeasure(COVERAGE, 0d))),
      newDoc("P32", "K1", "N1").setQualityGate(ERROR.name()).setMeasures(newArrayList(newMeasure(NCLOC, 5000d), newMeasure(COVERAGE, 40d))),
      newDoc("P33", "K1", "N1").setQualityGate(ERROR.name()).setMeasures(newArrayList(newMeasure(NCLOC, 12000d), newMeasure(COVERAGE, 50d))),
      newDoc("P34", "K1", "N1").setQualityGate(ERROR.name()).setMeasures(newArrayList(newMeasure(NCLOC, 13000d), newMeasure(COVERAGE, 60d))));

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
    userSession.login("john").setUserId(10);

    // User can see these projects
    addDocs(10L, null,
      // 2 docs with QG OK
      newDoc("P11", "K1", "N1").setQualityGate(OK.name()),
      newDoc("P12", "K1", "N1").setQualityGate(OK.name()),
      // 3 docs with QG WARN
      newDoc("P21", "K1", "N1").setQualityGate(WARN.name()),
      newDoc("P22", "K1", "N1").setQualityGate(WARN.name()),
      newDoc("P23", "K1", "N1").setQualityGate(WARN.name()));

    // User cannot see these projects
    addDocs(33L, null,
      // 4 docs with QG ERROR
      newDoc("P31", "K1", "N1").setQualityGate(ERROR.name()),
      newDoc("P32", "K1", "N1").setQualityGate(ERROR.name()),
      newDoc("P33", "K1", "N1").setQualityGate(ERROR.name()),
      newDoc("P34", "K1", "N1").setQualityGate(ERROR.name()));

    LinkedHashMap<String, Long> result = underTest.search(new ProjectMeasuresQuery(), new SearchOptions().addFacets(ALERT_STATUS_KEY)).getFacets().get(ALERT_STATUS_KEY);

    assertThat(result).containsExactly(
      entry(ERROR.name(), 0L),
      entry(WARN.name(), 3L),
      entry(OK.name(), 2L));
  }

  private void addDocs(ProjectMeasuresDoc... docs) {
    addDocs(null, ANYONE, docs);
  }

  private void addDocs(@Nullable Long authorizeUser, @Nullable String authorizedGroup, ProjectMeasuresDoc... docs) {
    try {
      es.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, docs);
      for (ProjectMeasuresDoc doc : docs) {
        authorizationIndexerTester.indexProjectPermission(doc.getId(),
          authorizedGroup != null ? singletonList(authorizedGroup) : emptyList(),
          authorizeUser != null ? singletonList(authorizeUser) : emptyList());
      }
    } catch (Exception e) {
      Throwables.propagate(e);
    }
  }

  private static ProjectMeasuresDoc newDoc(String uuid, String key, String name) {
    return new ProjectMeasuresDoc()
      .setId(uuid)
      .setKey(key)
      .setName(name);
  }

  private Map<String, Object> newMeasure(String key, Object value) {
    return ImmutableMap.of("key", key, "value", value);
  }
}
