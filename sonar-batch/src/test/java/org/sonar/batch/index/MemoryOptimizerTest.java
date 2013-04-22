/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.index;

import org.junit.Test;
import org.sonar.api.database.model.MeasureData;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class MemoryOptimizerTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldEvictDatabaseOnlyMeasure() {
    MemoryOptimizer optimizer = new MemoryOptimizer(getSession());
    Measure measure = new Measure(CoreMetrics.CONDITIONS_BY_LINE)
        .setData("10=23")
        .setPersistenceMode(PersistenceMode.DATABASE)
        .setId(12345L);
    MeasureModel model = newPersistedModel();

    optimizer.evictDataMeasure(measure, model);

    assertThat(optimizer.isTracked(12345L),is(true));
    assertThat(measure.getData(), nullValue());// data has been removed from memory
  }

  @Test
  public void shouldNotEvictStandardMeasure() {
    MemoryOptimizer optimizer = new MemoryOptimizer(getSession());
    Measure measure = new Measure(CoreMetrics.PROFILE)
        .setData("Sonar way")
        .setId(12345L);
    MeasureModel model = newPersistedModel();

    optimizer.evictDataMeasure(measure, model);

    assertThat(optimizer.isTracked(12345L),is(false));
    assertThat(measure.getData(), is("Sonar way"));
  }

  @Test
  public void shouldReloadEvictedMeasure() {
    setupData("shouldReloadEvictedMeasure");
    MemoryOptimizer optimizer = new MemoryOptimizer(getSession());
    Measure measure = new Measure(CoreMetrics.CONDITIONS_BY_LINE)
        .setData("initial")
        .setPersistenceMode(PersistenceMode.DATABASE)
        .setId(12345L);

    optimizer.evictDataMeasure(measure, newPersistedModel());
    assertThat(measure.getData(), nullValue());

    optimizer.reloadMeasure(measure);

    assertThat(measure.getData().length(), greaterThan(5));

    optimizer.flushMemory();
    assertThat(measure.getData(), nullValue());
  }

  private MeasureModel newPersistedModel() {
    MeasureModel model = new MeasureModel();
    model.setId(12345L);
    MeasureData measureData = new MeasureData();
    measureData.setId(500);
    model.setMeasureData(measureData);
    return model;
  }
}
