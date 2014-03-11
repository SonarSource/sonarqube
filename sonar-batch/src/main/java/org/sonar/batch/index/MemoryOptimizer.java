/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.events.DecoratorExecutionHandler;
import org.sonar.api.batch.events.DecoratorsPhaseHandler;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureData;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;

import java.util.List;
import java.util.Map;

/**
 * @since 2.7
 */
public class MemoryOptimizer implements SensorExecutionHandler, DecoratorExecutionHandler, DecoratorsPhaseHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryOptimizer.class);

  private List<Measure> loadedMeasures = Lists.newArrayList();
  private Map<Long, Integer> dataIdByMeasureId = Maps.newHashMap();
  private DatabaseSession session;

  public MemoryOptimizer(DatabaseSession session) {
    this.session = session;
  }

  /**
   * Remove data of a database measure from memory.
   */
  public void evictDataMeasure(Measure measure, MeasureModel model) {
    if (PersistenceMode.DATABASE.equals(measure.getPersistenceMode())) {
      MeasureData data = model.getMeasureData();
      if (data != null && data.getId() != null) {
        measure.unsetData();
        dataIdByMeasureId.put(measure.getId(), data.getId());
      }
    }
  }

  public Measure reloadMeasure(Measure measure) {
    if (measure.getId() != null && dataIdByMeasureId.containsKey(measure.getId()) && !measure.hasData()) {
      Integer dataId = dataIdByMeasureId.get(measure.getId());
      MeasureData data = session.getSingleResult(MeasureData.class, "id", dataId);
      if (data == null) {
        LOG.error("The MEASURE_DATA row with id {} is lost", dataId);

      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Reload the data measure: {}, id={}", measure.getMetricKey(), measure.getId());
        }
        measure.setData(data.getText());
        loadedMeasures.add(measure);
      }
    }
    return measure;
  }

  public void flushMemory() {
    if (LOG.isDebugEnabled() && !loadedMeasures.isEmpty()) {
      LOG.debug("Flush {} data measures from memory: ", loadedMeasures.size());
    }
    for (Measure measure : loadedMeasures) {
      measure.unsetData();
    }
    loadedMeasures.clear();
  }

  boolean isTracked(Long measureId) {
    return dataIdByMeasureId.get(measureId) != null;
  }

  public void onSensorExecution(SensorExecutionEvent event) {
    if (event.isEnd()) {
      flushMemory();
      session.commit();
    }
  }

  public void onDecoratorExecution(DecoratorExecutionEvent event) {
    if (event.isEnd()) {
      flushMemory();
    }
  }

  public void onDecoratorsPhase(DecoratorsPhaseEvent event) {
    if (event.isEnd()) {
      session.commit();
    }
  }

}
