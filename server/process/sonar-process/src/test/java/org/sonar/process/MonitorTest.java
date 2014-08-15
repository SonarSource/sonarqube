package org.sonar.process;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MonitorTest extends ProcessTest {


  Monitor monitor;

  @Before
  public void setUpMonitor() throws Exception {
    monitor = new Monitor();
  }

  @After
  public void downMonitor() throws Exception {
    if (monitor != null) {
      monitor.interrupt();
      monitor = null;
    }
  }

  @Test
  public void monitor_can_start_and_stop() {
    assertThat(monitor.isAlive()).isFalse();
    monitor.start();
    assertThat(monitor.isAlive()).isTrue();
    monitor.terminate();
    assertThat(monitor.isAlive()).isFalse();
  }

  @Test(timeout = 2500L)
  public void monitor_should_interrupt_process() throws Exception {
    // 0 start the dummyProcess
    ProcessWrapper process = new ProcessWrapper("DummyOkProcess")
      .addProperty(MonitoredProcess.NAME_PROPERTY, "DummyOkProcess")
      .addClasspath(dummyAppJar.getAbsolutePath())
      .setWorkDir(temp.getRoot())
      .setTempDirectory(temp.getRoot())
      .setJmxPort(freePort)
      .setClassName(DUMMY_OK_APP);

    assertThat(process.execute());


    // 1 start my monitor & register process
    monitor.start();
    monitor.registerProcess(process);

    // 2 terminate monitor, assert process is terminated
    monitor.terminate();
    assertThat(monitor.isAlive()).isFalse();
    assertThat(process.isAlive()).isFalse();
  }
}