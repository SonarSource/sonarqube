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
import java.util.Collections;
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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.measures.Metric.Level.OK;
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
      .addMetricCriterion(new MetricCriterion(NCLOC, Operator.GT, 10_000d));
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

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", 3L),
      entry("1000.0-10000.0", 2L),
      entry("10000.0-100000.0", 4L),
      entry("100000.0-500000.0", 2L),
      entry("500000.0-*", 5L));
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

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.get(COVERAGE)).containsExactly(
      entry("*-30.0", 3L),
      entry("30.0-50.0", 2L),
      entry("50.0-70.0", 4L),
      entry("70.0-80.0", 2L),
      entry("80.0-*", 5L));
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

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.get(DUPLICATION)).containsExactly(
      entry("*-3.0", 3L),
      entry("3.0-5.0", 2L),
      entry("5.0-10.0", 4L),
      entry("10.0-20.0", 2L),
      entry("20.0-*", 5L));
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

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.get(MAINTAINABILITY_RATING)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 2L),
      entry("5", 5L));
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

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

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

    Facets facets = underTest.search(new ProjectMeasuresQuery(), new SearchOptions()).getFacets();

    assertThat(facets.get(SECURITY_RATING)).containsExactly(
      entry("1", 3L),
      entry("2", 2L),
      entry("3", 4L),
      entry("4", 2L),
      entry("5", 5L));
  }

  private void addDocs(ProjectMeasuresDoc... docs) {
    addDocs(null, ANYONE, docs);
  }

  private void addDocs(@Nullable Long authorizeUser, @Nullable String authorizedGroup, ProjectMeasuresDoc... docs) {
    try {
      es.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, docs);
      for (ProjectMeasuresDoc doc : docs) {
        authorizationIndexerTester.indexProjectPermission(doc.getId(),
          authorizedGroup != null ? singletonList(authorizedGroup) : Collections.emptyList(),
          authorizeUser != null ? singletonList(authorizeUser) : Collections.emptyList());
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
