package org.sonar.process;

import com.sun.tools.attach.VirtualMachine;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class ProcessWrapperTest {

  int freePort;

  @Before
  public void setup() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    freePort = socket.getLocalPort();
    socket.close();
  }

  @Test
  @Ignore("Not a good idea to assert on # of VMs")
  public void process_should_run() throws IOException, MalformedObjectNameException, InterruptedException {

    int VMcount = VirtualMachine.list().size();
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    ProcessWrapper wrapper = wrapper = new ProcessWrapper(ProcessTest.TestProcess.class.getName(),
      Collections.EMPTY_MAP, "TEST", freePort, runtime.getClassPath());

    assertThat(wrapper).isNotNull();
    assertThat(wrapper.isReady()).isTrue();

    assertThat(VirtualMachine.list().size()).isEqualTo(VMcount + 1);

    wrapper.stop();
    assertThat(VirtualMachine.list().size()).isEqualTo(VMcount);

  }
}