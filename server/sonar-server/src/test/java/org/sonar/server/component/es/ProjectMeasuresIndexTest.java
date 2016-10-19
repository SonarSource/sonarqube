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
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.component.es.ProjectMeasuresQuery.Operator;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexTest {

  private static final String COVERAGE = "coverage";
  private static final String NCLOC = "ncloc";
  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  private ProjectMeasuresIndex underTest = new ProjectMeasuresIndex(es.client());

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

  private void addDocs(ProjectMeasuresDoc... docs) {
    try {
      es.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, docs);
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
