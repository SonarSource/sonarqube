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
package org.sonar.batch.profiling;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.events.*;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeUtils;
import org.sonar.batch.events.BatchStepHandler;
import org.sonar.batch.phases.Phases;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.sonar.batch.profiling.AbstractTimeProfiling.sortByDescendingTotalTime;
import static org.sonar.batch.profiling.AbstractTimeProfiling.truncate;

public class PhasesSumUpTimeProfiler implements ProjectAnalysisHandler, SensorExecutionHandler, DecoratorExecutionHandler, PostJobExecutionHandler, DecoratorsPhaseHandler,
  SensorsPhaseHandler, PostJobsPhaseHandler, MavenPhaseHandler, InitializersPhaseHandler, InitializerExecutionHandler, BatchStepHandler {

  static final Logger LOG = LoggerFactory.getLogger(PhasesSumUpTimeProfiler.class);
  private static final int TEXT_RIGHT_PAD = 60;
  private static final int TIME_LEFT_PAD = 10;

  @VisibleForTesting
  ModuleProfiling currentModuleProfiling;

  @VisibleForTesting
  ModuleProfiling totalProfiling;

  private Map<Project, ModuleProfiling> modulesProfilings = new HashMap<Project, ModuleProfiling>();
  private DecoratorsProfiler decoratorsProfiler;

  private final System2 system;

  public PhasesSumUpTimeProfiler() {
    this(System2.INSTANCE);
  }

  static void println(String msg) {
    LOG.info(msg);
  }

  static void println(String text, @Nullable Double percent, AbstractTimeProfiling phaseProfiling) {
    StringBuilder sb = new StringBuilder();
    sb.append(StringUtils.rightPad(text, TEXT_RIGHT_PAD)).append(StringUtils.leftPad(phaseProfiling.totalTimeAsString(), TIME_LEFT_PAD));
    if (percent != null) {
      sb.append(" (").append((int) (phaseProfiling.totalTime() / percent)).append("%)");
    }
    println(sb.toString());
  }

  @VisibleForTesting
  PhasesSumUpTimeProfiler(System2 system) {
    this.totalProfiling = new ModuleProfiling(null, system);
    this.system = system;
  }

  @Override
  public void onProjectAnalysis(ProjectAnalysisEvent event) {
    Project module = event.getProject();
    if (event.isStart()) {
      decoratorsProfiler = new DecoratorsProfiler();
      currentModuleProfiling = new ModuleProfiling(module, system);
    } else {
      currentModuleProfiling.stop();
      modulesProfilings.put(module, currentModuleProfiling);
      long moduleTotalTime = currentModuleProfiling.totalTime();
      println("");
      println(" -------- Profiling of module " + module.getName() + ": " + TimeUtils.formatDuration(moduleTotalTime) + " --------");
      println("");
      currentModuleProfiling.dump();
      println("");
      println(" -------- End of profiling of module " + module.getName() + " --------");
      println("");
      totalProfiling.merge(currentModuleProfiling);
      if (module.isRoot() && !module.getModules().isEmpty()) {
        dumpTotalExecutionSummary();
      }
    }
  }

  private void dumpTotalExecutionSummary() {
    totalProfiling.stop();
    long totalTime = totalProfiling.totalTime();
    println("");
    println(" ======== Profiling of total execution: " + TimeUtils.formatDuration(totalTime) + " ========");
    println("");
    println(" * Module execution time breakdown: ");
    double percent = totalTime / 100.0;
    for (ModuleProfiling modulesProfiling : truncate(sortByDescendingTotalTime(modulesProfilings).values())) {
      println("   o " + modulesProfiling.moduleName() + " execution time: ", percent, modulesProfiling);
    }
    println("");
    totalProfiling.dump();
    println("");
    println(" ======== End of profiling of total execution ========");
    println("");
  }

  public void onSensorsPhase(SensorsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phases.Phase.SENSOR);
    } else {
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
    } else {
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
    } else {
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

  @Override
  public void onMavenPhase(MavenPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phases.Phase.MAVEN);
    } else {
      currentModuleProfiling.getProfilingPerPhase(Phases.Phase.MAVEN).stop();
    }
  }

  @Override
  public void onInitializersPhase(InitializersPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phases.Phase.INIT);
    } else {
      currentModuleProfiling.getProfilingPerPhase(Phases.Phase.INIT).stop();
    }
  }

  @Override
  public void onInitializerExecution(InitializerExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phases.Phase.INIT);
    if (event.isStart()) {
      profiling.newItemProfiling(event.getInitializer());
    } else {
      profiling.getProfilingPerItem(event.getInitializer()).stop();
    }
  }

  @Override
  public void onBatchStep(BatchStepEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addBatchStepProfiling(event.stepName());
    } else {
      currentModuleProfiling.getProfilingPerBatchStep(event.stepName()).stop();
    }
  }

  class DecoratorsProfiler {
    private List<Decorator> decorators = Lists.newArrayList();
    private Map<Decorator, Long> durations = new IdentityHashMap<Decorator, Long>();
    private long startTime;
    private Decorator currentDecorator;

    DecoratorsProfiler() {
    }

    void start(Decorator decorator) {
      this.startTime = system.now();
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
      durations.put(currentDecorator, cumulatedDuration + (system.now() - startTime));
    }

    public Map<Decorator, Long> getDurations() {
      return durations;
    }

  }

}
