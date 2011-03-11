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
package org.sonar.batch.phases;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.events.DecoratorExecutionEvent;
import org.sonar.batch.events.DecoratorExecutionHandler;
import org.sonar.batch.events.DecoratorsPhaseEvent;
import org.sonar.batch.events.DecoratorsPhaseHandler;
import org.sonar.batch.events.SensorExecutionEvent;
import org.sonar.batch.events.SensorExecutionHandler;
import org.sonar.batch.events.SensorsPhaseEvent;
import org.sonar.batch.events.SensorsPhaseHandler;

public class PhasesTimeProfiler implements SensorExecutionHandler, DecoratorExecutionHandler, DecoratorsPhaseHandler, SensorsPhaseHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PhasesTimeProfiler.class);

  private TimeProfiler profiler = new TimeProfiler(LOG);
  private DecoratorsProfiler decoratorsProfiler = new DecoratorsProfiler();

  public void onSensorsPhase(SensorsPhaseEvent event) {
    if (event.isPhaseStart()) {
      LOG.debug("Sensors : {}", StringUtils.join(event.getSensors(), " -> "));
    }
  }

  public void onSensorExecution(SensorExecutionEvent event) {
    if (event.isStartExecution()) {
      profiler.start("Sensor " + event.getSensor());
    } else {
      profiler.stop();
    }
  }

  public void onDecoratorExecution(DecoratorExecutionEvent event) {
    if (event.isStartExecution()) {
      decoratorsProfiler.start(event.getDecorator());
    } else {
      decoratorsProfiler.stop();
    }
  }

  public void onDecoratorsPhase(DecoratorsPhaseEvent event) {
    if (event.isPhaseStart()) {
      LOG.info("Execute decorators...");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Decorators: {}", StringUtils.join(event.getDecorators(), " -> "));
      }
    } else {
      decoratorsProfiler.log();
    }
  }

  static class DecoratorsProfiler {
    List<Decorator> decorators = Lists.newArrayList();
    Map<Decorator, Long> durations = new IdentityHashMap<Decorator, Long>();
    long startTime;
    Decorator currentDecorator;

    DecoratorsProfiler() {
    }

    void start(Decorator decorator) {
      this.startTime = System.currentTimeMillis();
      this.currentDecorator = decorator;
    }

    void stop() {
      final Long cumulatedDuration;
      if (durations.containsKey(currentDecorator)) {
        cumulatedDuration = durations.get(currentDecorator);
      } else {
        decorators.add(currentDecorator);
        cumulatedDuration = 0L;
      }
      durations.put(currentDecorator, cumulatedDuration + (System.currentTimeMillis() - startTime));
    }

    void log() {
      LOG.debug(getMessage());
    }

    String getMessage() {
      StringBuilder sb = new StringBuilder("Decorator time:").append(SystemUtils.LINE_SEPARATOR);
      for (Decorator decorator : decorators) {
        sb.append("\t").append(decorator.toString()).append(": ").append(durations.get(decorator)).append("ms")
            .append(SystemUtils.LINE_SEPARATOR);
      }
      return sb.toString();
    }
  }

}
