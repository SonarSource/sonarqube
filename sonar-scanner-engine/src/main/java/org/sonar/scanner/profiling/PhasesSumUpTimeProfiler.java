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
package org.sonar.scanner.profiling;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.events.DecoratorExecutionHandler;
import org.sonar.api.batch.events.DecoratorsPhaseHandler;
import org.sonar.api.batch.events.InitializerExecutionHandler;
import org.sonar.api.batch.events.InitializersPhaseHandler;
import org.sonar.api.batch.events.PostJobExecutionHandler;
import org.sonar.api.batch.events.PostJobsPhaseHandler;
import org.sonar.api.batch.events.ProjectAnalysisHandler;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.batch.events.SensorsPhaseHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeUtils;
import org.sonar.scanner.bootstrap.GlobalProperties;
import org.sonar.scanner.events.BatchStepHandler;
import org.sonar.scanner.util.BatchUtils;

import static org.sonar.scanner.profiling.AbstractTimeProfiling.sortByDescendingTotalTime;
import static org.sonar.scanner.profiling.AbstractTimeProfiling.truncate;

public class PhasesSumUpTimeProfiler implements ProjectAnalysisHandler, SensorExecutionHandler, DecoratorExecutionHandler, PostJobExecutionHandler, DecoratorsPhaseHandler,
  SensorsPhaseHandler, PostJobsPhaseHandler, InitializersPhaseHandler, InitializerExecutionHandler, BatchStepHandler {

  static final Logger LOG = LoggerFactory.getLogger(PhasesSumUpTimeProfiler.class);
  private static final int TEXT_RIGHT_PAD = 60;
  private static final int TIME_LEFT_PAD = 10;

  @VisibleForTesting
  ModuleProfiling currentModuleProfiling;

  @VisibleForTesting
  ModuleProfiling totalProfiling;

  private Map<Project, ModuleProfiling> modulesProfilings = new HashMap<>();
  private DecoratorsProfiler decoratorsProfiler;

  private final System2 system;
  private final File out;
  
  public PhasesSumUpTimeProfiler(System2 system, GlobalProperties bootstrapProps) {
    String workingDirPath = StringUtils.defaultIfBlank(bootstrapProps.property(CoreProperties.WORKING_DIRECTORY), CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE);
    File workingDir = new File(workingDirPath).getAbsoluteFile();
    this.out = new File(workingDir, "profiling");
    this.out.mkdirs();
    this.totalProfiling = new ModuleProfiling(null, system);
    this.system = system;
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
      Properties props = new Properties();
      currentModuleProfiling.dump(props);
      println("");
      println(" -------- End of profiling of module " + module.getName() + " --------");
      println("");
      String fileName = module.getKey() + "-profiler.properties";
      dumpToFile(props, BatchUtils.cleanKeyForFilename(fileName));
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
    Properties props = new Properties();
    totalProfiling.dump(props);
    println("");
    println(" ======== End of profiling of total execution ========");
    println("");
    String fileName = "total-execution-profiler.properties";
    dumpToFile(props, fileName);
  }

  private void dumpToFile(Properties props, String fileName) {
    File file = new File(out, fileName);
    try (FileOutputStream fos = new FileOutputStream(file)) {
      props.store(fos, "SonarQube");
      println("Profiling data stored in " + file.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to store profiler output: " + file, e);
    }
  }

  @Override
  public void onSensorsPhase(SensorsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phase.SENSOR);
    } else {
      currentModuleProfiling.getProfilingPerPhase(Phase.SENSOR).stop();
    }
  }

  @Override
  public void onSensorExecution(SensorExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phase.SENSOR);
    if (event.isStart()) {
      profiling.newItemProfiling(event.getSensor());
    } else {
      profiling.getProfilingPerItem(event.getSensor()).stop();
    }
  }

  @Override
  public void onDecoratorExecution(DecoratorExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR);
    if (event.isStart()) {
      if (profiling.getProfilingPerItem(event.getDecorator()) == null) {
        profiling.newItemProfiling(event.getDecorator());
      }
      decoratorsProfiler.start(event.getDecorator());
    } else {
      decoratorsProfiler.stop();
    }
  }

  @Override
  public void onDecoratorsPhase(DecoratorsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phase.DECORATOR);
    } else {
      for (Decorator decorator : decoratorsProfiler.getDurations().keySet()) {
        currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR)
          .getProfilingPerItem(decorator).setTotalTime(decoratorsProfiler.getDurations().get(decorator));
      }
      currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).stop();
    }
  }

  @Override
  public void onPostJobsPhase(PostJobsPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phase.POSTJOB);
    } else {
      currentModuleProfiling.getProfilingPerPhase(Phase.POSTJOB).stop();
    }
  }

  @Override
  public void onPostJobExecution(PostJobExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phase.POSTJOB);
    if (event.isStart()) {
      profiling.newItemProfiling(event.getPostJob());
    } else {
      profiling.getProfilingPerItem(event.getPostJob()).stop();
    }
  }

  @Override
  public void onInitializersPhase(InitializersPhaseEvent event) {
    if (event.isStart()) {
      currentModuleProfiling.addPhaseProfiling(Phase.INIT);
    } else {
      currentModuleProfiling.getProfilingPerPhase(Phase.INIT).stop();
    }
  }

  @Override
  public void onInitializerExecution(InitializerExecutionEvent event) {
    PhaseProfiling profiling = currentModuleProfiling.getProfilingPerPhase(Phase.INIT);
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
    private Map<Decorator, Long> durations = new IdentityHashMap<>();
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
