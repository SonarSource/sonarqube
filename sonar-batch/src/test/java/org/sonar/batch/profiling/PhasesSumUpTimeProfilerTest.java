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
package org.sonar.batch.profiling;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.events.DecoratorExecutionHandler;
import org.sonar.api.batch.events.DecoratorsPhaseHandler;
import org.sonar.api.batch.events.InitializerExecutionHandler;
import org.sonar.api.batch.events.InitializersPhaseHandler;
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
import org.sonar.api.utils.System2;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.events.BatchStepEvent;
import org.sonar.batch.index.ScanPersister;
import org.sonar.batch.phases.Phases.Phase;
import org.sonar.batch.phases.event.PersisterExecutionHandler;
import org.sonar.batch.phases.event.PersistersPhaseHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PhasesSumUpTimeProfilerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MockedSystem clock;
  private PhasesSumUpTimeProfiler profiler;

  @Before
  public void prepare() throws Exception {
    clock = new MockedSystem();
    Map<String, String> props = Maps.newHashMap();
    props.put(CoreProperties.WORKING_DIRECTORY, temp.newFolder().getAbsolutePath());
    profiler = new PhasesSumUpTimeProfiler(clock, new BootstrapProperties(props));
  }

  @Test
  public void testSimpleProject() throws InterruptedException {

    final Project project = mockProject("my:project", true);
    when(project.getModules()).thenReturn(Collections.<Project>emptyList());

    fakeAnalysis(profiler, project);

    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.INIT).getProfilingPerItem(new FakeInitializer()).totalTime()).isEqualTo(7L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.SENSOR).getProfilingPerItem(new FakeSensor()).totalTime()).isEqualTo(10L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator1()).totalTime()).isEqualTo(20L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.PERSISTER).getProfilingPerItem(new FakeScanPersister()).totalTime()).isEqualTo(40L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.POSTJOB).getProfilingPerItem(new FakePostJob()).totalTime()).isEqualTo(30L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerBatchStep("Free memory").totalTime()).isEqualTo(9L);

  }

  @Test
  public void testMultimoduleProject() throws InterruptedException {
    final Project project = mockProject("project root", true);
    final Project moduleA = mockProject("moduleA", false);
    final Project moduleB = mockProject("moduleB", false);
    when(project.getModules()).thenReturn(Arrays.asList(moduleA, moduleB));

    fakeAnalysis(profiler, moduleA);
    fakeAnalysis(profiler, moduleB);
    fakeAnalysis(profiler, project);

    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.INIT).getProfilingPerItem(new FakeInitializer()).totalTime()).isEqualTo(7L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.SENSOR).getProfilingPerItem(new FakeSensor()).totalTime()).isEqualTo(10L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator1()).totalTime()).isEqualTo(20L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator2()).totalTime()).isEqualTo(10L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.PERSISTER).getProfilingPerItem(new FakeScanPersister()).totalTime()).isEqualTo(40L);
    assertThat(profiler.currentModuleProfiling.getProfilingPerPhase(Phase.POSTJOB).getProfilingPerItem(new FakePostJob()).totalTime()).isEqualTo(30L);

    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.INIT).getProfilingPerItem(new FakeInitializer()).totalTime()).isEqualTo(21L);
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.SENSOR).getProfilingPerItem(new FakeSensor()).totalTime()).isEqualTo(30L);
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator1()).totalTime()).isEqualTo(60L);
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.DECORATOR).getProfilingPerItem(new FakeDecorator2()).totalTime()).isEqualTo(30L);
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.PERSISTER).getProfilingPerItem(new FakeScanPersister()).totalTime()).isEqualTo(120L);
    assertThat(profiler.totalProfiling.getProfilingPerPhase(Phase.POSTJOB).getProfilingPerItem(new FakePostJob()).totalTime()).isEqualTo(90L);
  }

  @Test
  public void testDisplayTimings() {
    AbstractTimeProfiling profiling = new AbstractTimeProfiling(System2.INSTANCE) {
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

  private class MockedSystem extends System2 {
    private long now = 0;

    @Override
    public long now() {
      return now;
    }

    public void sleep(long duration) {
      now += duration;
    }
  }

  private Project mockProject(String name, boolean isRoot) {
    final Project project = spy(new Project("myProject"));
    when(project.isRoot()).thenReturn(isRoot);
    when(project.getName()).thenReturn(name);
    return project;
  }

  private void fakeAnalysis(PhasesSumUpTimeProfiler profiler, final Project module) throws InterruptedException {
    // Start of moduleA
    profiler.onProjectAnalysis(projectEvent(module, true));
    initializerPhase(profiler);
    sensorPhase(profiler);
    decoratorPhase(profiler);
    persistersPhase(profiler);
    postJobPhase(profiler);
    batchStep(profiler);
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
    clock.sleep(10);
    // End of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, false));
    // Start of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, true));
    clock.sleep(5);
    // End of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, false));
    // Start of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, true));
    clock.sleep(10);
    // End of decorator 1
    profiler.onDecoratorExecution(decoratorEvent(decorator1, false));
    // Start of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, true));
    clock.sleep(5);
    // End of decorator 2
    profiler.onDecoratorExecution(decoratorEvent(decorator2, false));
    // End of decorator phase
    profiler.onDecoratorsPhase(decoratorsEvent(false));
  }

  private void batchStep(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    // Start of batch step
    profiler.onBatchStep(new BatchStepEvent("Free memory", true));
    clock.sleep(9);
    // End of batch step
    profiler.onBatchStep(new BatchStepEvent("Free memory", false));
  }

  private void initializerPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    Initializer initializer = new FakeInitializer();
    // Start of initializer phase
    profiler.onInitializersPhase(initializersEvent(true));
    // Start of an initializer
    profiler.onInitializerExecution(initializerEvent(initializer, true));
    clock.sleep(7);
    // End of an initializer
    profiler.onInitializerExecution(initializerEvent(initializer, false));
    // End of initializer phase
    profiler.onInitializersPhase(initializersEvent(false));
  }

  private void sensorPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    Sensor sensor = new FakeSensor();
    // Start of sensor phase
    profiler.onSensorsPhase(sensorsEvent(true));
    // Start of a Sensor
    profiler.onSensorExecution(sensorEvent(sensor, true));
    clock.sleep(10);
    // End of a Sensor
    profiler.onSensorExecution(sensorEvent(sensor, false));
    // End of sensor phase
    profiler.onSensorsPhase(sensorsEvent(false));
  }

  private void persistersPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    ScanPersister persister = new FakeScanPersister();
    // Start of persister phase
    profiler.onPersistersPhase(persistersEvent(true));
    // Start of a ScanPersister
    profiler.onPersisterExecution(persisterEvent(persister, true));
    clock.sleep(40);
    // End of a ScanPersister
    profiler.onPersisterExecution(persisterEvent(persister, false));
    // End of persister phase
    profiler.onPersistersPhase(persistersEvent(false));
  }

  private void postJobPhase(PhasesSumUpTimeProfiler profiler) throws InterruptedException {
    PostJob postJob = new FakePostJob();
    // Start of sensor phase
    profiler.onPostJobsPhase(postJobsEvent(true));
    // Start of a Sensor
    profiler.onPostJobExecution(postJobEvent(postJob, true));
    clock.sleep(30);
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

  private InitializerExecutionHandler.InitializerExecutionEvent initializerEvent(final Initializer initializer, final boolean start) {
    return new InitializerExecutionHandler.InitializerExecutionEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public Initializer getInitializer() {
        return initializer;
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

  private PersisterExecutionHandler.PersisterExecutionEvent persisterEvent(final ScanPersister persister, final boolean start) {
    return new PersisterExecutionHandler.PersisterExecutionEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public ScanPersister getPersister() {
        return persister;
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

  private InitializersPhaseHandler.InitializersPhaseEvent initializersEvent(final boolean start) {
    return new InitializersPhaseHandler.InitializersPhaseEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public List<Initializer> getInitializers() {
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

  private PersistersPhaseHandler.PersistersPhaseEvent persistersEvent(final boolean start) {
    return new PersistersPhaseHandler.PersistersPhaseEvent() {

      @Override
      public boolean isStart() {
        return start;
      }

      @Override
      public boolean isEnd() {
        return !start;
      }

      @Override
      public List<ScanPersister> getPersisters() {
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

    @Override
    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakeInitializer extends Initializer {
    @Override
    public void execute(Project project) {
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakeDecorator1 implements Decorator {
    @Override
    public void decorate(Resource resource, DecoratorContext context) {
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakeDecorator2 implements Decorator {
    @Override
    public void decorate(Resource resource, DecoratorContext context) {
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakePostJob implements PostJob {
    @Override
    public void executeOn(Project project, SensorContext context) {
    }
  }

  public class FakeScanPersister implements ScanPersister {
    @Override
    public void persist() {
    }
  }
}
