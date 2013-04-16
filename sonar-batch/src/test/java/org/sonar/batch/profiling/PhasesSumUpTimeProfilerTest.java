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

import org.junit.Test;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.events.DecoratorExecutionHandler;
import org.sonar.api.batch.events.DecoratorsPhaseHandler;
import org.sonar.api.batch.events.PostJobExecutionHandler;
import org.sonar.api.batch.events.PostJobsPhaseHandler;
import org.sonar.api.batch.events.ProjectAnalysisHandler;
import org.sonar.api.batch.events.ProjectAnalysisHandler.ProjectAnalysisEvent;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.batch.events.SensorExecutionHandler.SensorExecutionEvent;
import org.sonar.api.batch.events.SensorsPhaseHandler;
import org.sonar.api.batch.events.SensorsPhaseHandler.SensorsPhaseEvent;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.phases.Phases.Phase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhasesSumUpTimeProfilerTest {

  @Test
  public void testSimpleProject() throws InterruptedException {
    PhasesSumUpTimeProfiler profiler = new PhasesSumUpTimeProfiler();
    final Project project = mockProject("project", true);
    when(project.getModules()).thenReturn(Collections.<Project> emptyList());

    fakeAnalysis(profiler, project);

    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.SENSOR).getProfilingPerItem(new FakeSensor()).totalTime()).isIn(delta(10L, 5));
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator1()).totalTime()).isIn(delta(20L, 5));
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.POSTJOB).getProfilingPerItem(new FakePostJob()).totalTime()).isIn(delta(30L, 5));

  }

  @Test
  public void testMultimoduleProject() throws InterruptedException {
    PhasesSumUpTimeProfiler profiler = new PhasesSumUpTimeProfiler();
    final Project project = mockProject("project root", true);
    final Project moduleA = mockProject("moduleA", false);
    final Project moduleB = mockProject("moduleB", false);
    when(project.getModules()).thenReturn(Arrays.asList(moduleA, moduleB));

    fakeAnalysis(profiler, moduleA);
    fakeAnalysis(profiler, moduleB);
    fakeAnalysis(profiler, project);

    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.SENSOR).getProfilingPerItem(new FakeSensor()).totalTime()).isIn(delta(10L, 5));
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator1()).totalTime()).isIn(delta(20L, 5));
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator2()).totalTime()).isIn(delta(10L, 5));
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.POSTJOB).getProfilingPerItem(new FakePostJob()).totalTime()).isIn(delta(30L, 5));

    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.SENSOR).getProfilingPerItem(new FakeSensor()).totalTime()).isIn(delta(30L, 5));
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator1()).totalTime()).isIn(delta(60L, 10));
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator2()).totalTime()).isIn(delta(30L, 5));
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.POSTJOB).getProfilingPerItem(new FakePostJob()).totalTime()).isIn(delta(90L, 10));
  }

  @Test
  public void testDisplayTimings() {
    AbstractTimeProfiling profiling = new AbstractTimeProfiling() {
    };

    profiling.setTotalTime(5);
    assertThat(profiling.totalTimeAsString()).isEqualTo("5ms");

    profiling.setTotalTime(5 * 1000 + 12);
    assertThat(profiling.totalTimeAsString()).isEqualTo("5s");

    profiling.setTotalTime(5 * 60 * 1000 + 12 * 1000);
    assertThat(profiling.totalTimeAsString()).isEqualTo("5min 12s");

    profiling.setTotalTime(5 * 60 * 1000);
    assertThat(profiling.totalTimeAsString()).isEqualTo("5min");
  }

  private Object[] delta(long value, int delta) {
    Long[] result = new Long[2 * delta + 1];
    int index = 0;
    for (long i = value - delta; i <= value + delta; i++) {
      result[index++] = i;
    }
    return result;
  }

  private Project mockProject(String name, boolean isRoot) {
    final Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(isRoot);
    when(project.getName()).thenReturn(name);
    return project;
  }

  private void fakeAnalysis(PhasesSumUpTimeProfiler profiler, final Project module) throws InterruptedException {
    // Start of moduleA
    profiler.onProjectAnalysis(projectEvent(module, true));
    sensorPhase(profiler);
    decoratorPhase(profiler);
    postJobPhase(profiler);
    // End of moduleA
    profiler.onProjectAnalysis(projectEvent(module, false));
  }

  private void decoratorPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    Decorator decorator1 = new FakeDecorator1();
    Decorator decorator2 = new FakeDecorator2();
    // Start of decorator phase
    profiler.onDecoratorsPhase(decoratorsEvent(true));
    // Start of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, true));
    Thread.sleep(10);
    // End of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, false));
    // Start of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, true));
    Thread.sleep(5);
    // End of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, false));
    // Start of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, true));
    Thread.sleep(10);
    // End of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, false));
    // Start of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, true));
    Thread.sleep(5);
    // End of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, false));
    // End of decorator phase
    profiler.onDecoratorsPhase(decoratorsEvent(false));
  }

  private void sensorPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    Sensor sensor = new FakeSensor();
    // Start of sensor phase
    profiler.onSensorsPhase(sensorsEvent(true));
    // Start of a Sensor
    profiler.onSensorExecution(sensorEvent(sensor, true));
    Thread.sleep(10);
    // End of a Sensor
    profiler.onSensorExecution(sensorEvent(sensor, false));
    // End of sensor phase
    profiler.onSensorsPhase(sensorsEvent(false));
  }

  private void postJobPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    PostJob postJob = new FakePostJob();
    // Start of sensor phase
    profiler.onPostJobsPhase(postJobsEvent(true));
    // Start of a Sensor
    profiler.onPostJobExecution(postJobEvent(postJob, true));
    Thread.sleep(30);
    // End of a Sensor
    profiler.onPostJobExecution(postJobEvent(postJob, false));
    // End of sensor phase
    profiler.onPostJobsPhase(postJobsEvent(false));
  }

  private SensorExecutionEvent sensorEvent(final Sensor sensor, final boolean start) {
    return new SensorExecutionHandler.SensorExecutionEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public Sensor getSensor() {
        return sensor;
      }
    };
  }

  private DecoratorExecutionHandler.DecoratorExecutionEvent decoratorEvent(final Decorator decorator, final boolean start) {
    return new DecoratorExecutionHandler.DecoratorExecutionEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public Decorator getDecorator() {
        return decorator;
      }
    };
  }

  private PostJobExecutionHandler.PostJobExecutionEvent postJobEvent(final PostJob postJob, final boolean start) {
    return new PostJobExecutionHandler.PostJobExecutionEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public PostJob getPostJob() {
        return postJob;
      }
    };
  }

  private SensorsPhaseEvent sensorsEvent(final boolean start) {
    return new SensorsPhaseHandler.SensorsPhaseEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public List<Sensor> getSensors() {
        return null;
      }
    };
  }

  private PostJobsPhaseHandler.PostJobsPhaseEvent postJobsEvent(final boolean start) {
    return new PostJobsPhaseHandler.PostJobsPhaseEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public List<PostJob> getPostJobs() {
        return null;
      }
    };
  }

  private DecoratorsPhaseHandler.DecoratorsPhaseEvent decoratorsEvent(final boolean start) {
    return new DecoratorsPhaseHandler.DecoratorsPhaseEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public List<Decorator> getDecorators() {
        return null;
      }
    };
  }

  private ProjectAnalysisEvent projectEvent(final Project project, final boolean start) {
    return new ProjectAnalysisHandler.ProjectAnalysisEvent() {
      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public Project getProject() {
        return project;
      }
    };
  }

  public class FakeSensor implements Sensor {
    @Override
    public void analyse(Project project, SensorContext context) {
    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakeDecorator1 implements Decorator {
    public void decorate(Resource resource, DecoratorContext context) {
    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakeDecorator2 implements Decorator {
    public void decorate(Resource resource, DecoratorContext context) {
    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakePostJob implements PostJob {
    @Override
    public void executeOn(Project project, SensorContext context) {
    }
  }
}
