/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.profiling;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.events.DecoratorExecutionHandler;
import org.sonar.api.batch.events.DecoratorsPhaseHandler;
import org.sonar.api.batch.events.PostJobExecutionHandler;
import org.sonar.api.batch.events.PostJobsPhaseHandler;
import org.sonar.api.batch.events.ProjectAnalysisHandler;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.batch.events.SensorsPhaseHandler;
import org.sonar.api.resources.Project;
import org.sonar.batch.phases.Phases;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class PhasesSumUpTimeProfiler implements ProjectAnalysisHandler, SensorExecutionHandler, DecoratorExecutionHandler, PostJobExecutionHandler, DecoratorsPhaseHandler,
    SensorsPhaseHandler, PostJobsPhaseHandler {

  @VisibleForTesting
  ModuleProfiling currentModuleProfiling;
  @VisibleForTesting
  ModuleProfiling totalProfiling;
  private DecoratorsProfiler decoratorsProfiler;

  private Clock clock;

  public PhasesSumUpTimeProfiler() {
    this(new Clock());
  }

  @VisibleForTesting
  PhasesSumUpTimeProfiler(Clock clock) {
    this.clock = clock;
    totalProfiling = new ModuleProfiling(clock);
  }

  @Override
  public void onProjectAnalysis(ProjectAnalysisEvent event) {
    Project module = event.getProject();
    if (event.isStart()) {
      decoratorsProfiler = new DecoratorsProfiler();
      currentModuleProfiling = new ModuleProfiling(clock);
    }
    else {
      currentModuleProfiling.stop();
      System.out.println("\n -------- Profiling for module " + module.getName() + " --------\n");
      currentModuleProfiling.dump();
      System.out.println("\n -------- End of profiling for module " + module.getName() + " --------\n");
      totalProfiling.merge(currentModuleProfiling);
      if (module.isRoot() && !module.getModules().isEmpty()) {
        totalProfiling.stop();
        System.out.println("\n ======== Profiling for total execution ========\n");
        totalProfiling.dump();
        System.out.println("\n ======== End of profiling for total execution ========\n");
      }
    }
  }

  public void onSensorsPhase(SensorsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phases.Phase.SENSOR);
    }
    else {
      currentModuleProfiling.getProfilingPerPhase(Phases.Phase.SENSOR).stop();
    }
  }

  public void onSensorExecution(SensorExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phases.Phase.SENSOR);
    if (event.isStart()) {
      profiling.newItemProfiling(event.getSensor());
    } else {
      profiling.getProfilingPerItem(event.getSensor()).stop();
    }
  }

  public void onDecoratorExecution(DecoratorExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phases.Phase.DECORATOR);
    if (event.isStart()) {
      if (profiling.getProfilingPerItem(event.getDecorator()) == null) {
        profiling.newItemProfiling(event.getDecorator());
      }
      decoratorsProfiler.start(event.getDecorator());
    } else {
      decoratorsProfiler.stop();
    }
  }

  public void onDecoratorsPhase(DecoratorsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phases.Phase.DECORATOR);
    }
    else {
      for (Decorator decorator : decoratorsProfiler.getDurations().keySet()) {
        currentModuleProfiling.getProfilingPerPhase(Phases.Phase.DECORATOR)
            .getProfilingPerItem(decorator).setTotalTime(decoratorsProfiler.getDurations().get(decorator));
      }
      currentModuleProfiling.getProfilingPerPhase(Phases.Phase.DECORATOR).stop();
    }
  }

  public void onPostJobsPhase(PostJobsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phases.Phase.POSTJOB);
    }
    else {
      currentModuleProfiling.getProfilingPerPhase(Phases.Phase.POSTJOB).stop();
    }
  }

  public void onPostJobExecution(PostJobExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phases.Phase.POSTJOB);
    if (event.isStart()) {
      profiling.newItemProfiling(event.getPostJob());
    } else {
      profiling.getProfilingPerItem(event.getPostJob()).stop();
    }
  }

  class DecoratorsProfiler {
    List<Decorator> decorators = Lists.newArrayList();
    Map<Decorator, Long> durations = new IdentityHashMap<Decorator, Long>();
    long startTime;
    Decorator currentDecorator;

    DecoratorsProfiler() {
    }

    void start(Decorator decorator) {
      this.startTime = clock.now();
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
      durations.put(currentDecorator, cumulatedDuration + (clock.now() - startTime));
    }

    public Map<Decorator, Long> getDurations() {
      return durations;
    }

  }

}
