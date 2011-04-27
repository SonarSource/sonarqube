/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.components;

import org.junit.Test;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Metric;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class PastMeasuresLoaderTest extends AbstractDbUnitTestCase {

  private static final int PROJECT_SNAPSHOT_ID = 1000;
  private static final int PROJECT_ID = 1;
  private static final int FILE_ID = 3;

  @Test
  public void shouldGetPastResourceMeasures() {
    setupData("shared");

    List<Metric> metrics = selectMetrics();
    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", PROJECT_SNAPSHOT_ID);

    PastMeasuresLoader loader = new PastMeasuresLoader(getSession(), metrics);
    List<Object[]> measures = loader.getPastMeasures(FILE_ID, projectSnapshot);
    assertThat(measures.size(), is(2));

    Object[] pastMeasure = measures.get(0);
    assertThat(PastMeasuresLoader.getMetricId(pastMeasure), is(1));
    assertThat(PastMeasuresLoader.getCharacteristicId(pastMeasure), nullValue());
    assertThat(PastMeasuresLoader.getValue(pastMeasure), is(5.0));

    pastMeasure = measures.get(1);
    assertThat(PastMeasuresLoader.getMetricId(pastMeasure), is(2));
    assertThat(PastMeasuresLoader.getCharacteristicId(pastMeasure), nullValue());
    assertThat(PastMeasuresLoader.getValue(pastMeasure), is(60.0));
  }

  @Test
  public void shouldGetPastProjectMeasures() {
    setupData("shared");

    List<Metric> metrics = selectMetrics();
    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", PROJECT_SNAPSHOT_ID);

    PastMeasuresLoader loader = new PastMeasuresLoader(getSession(), metrics);
    List<Object[]> measures = loader.getPastMeasures(PROJECT_ID, projectSnapshot);
    assertThat(measures.size(), is(2));

    Object[] pastMeasure = measures.get(0);
    assertThat(PastMeasuresLoader.getMetricId(pastMeasure), is(1));
    assertThat(PastMeasuresLoader.getCharacteristicId(pastMeasure), nullValue());
    assertThat(PastMeasuresLoader.getValue(pastMeasure), is(60.0));

    pastMeasure = measures.get(1);
    assertThat(PastMeasuresLoader.getMetricId(pastMeasure), is(2));
    assertThat(PastMeasuresLoader.getCharacteristicId(pastMeasure), nullValue());
    assertThat(PastMeasuresLoader.getValue(pastMeasure), is(80.0));
  }

  @Test
  public void shouldKeepOnlyNumericalMetrics() {
    Metric ncloc = new Metric("ncloc", Metric.ValueType.INT);
    ncloc.setId(1);
    Metric complexity = new Metric("complexity", Metric.ValueType.INT);
    complexity.setId(2);
    Metric data = new Metric("data", Metric.ValueType.DATA);
    data.setId(3);
    List<Metric> metrics = Arrays.asList(ncloc, complexity, data);

    PastMeasuresLoader loader = new PastMeasuresLoader(getSession(), metrics);
    
    assertThat(loader.getMetrics().size(), is(2));
    assertThat(loader.getMetrics(), hasItems(ncloc, complexity));
  }

  private List<Metric> selectMetrics() {
    return getSession().getResults(Metric.class);
  }
}
