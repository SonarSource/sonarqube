package org.sonar.process;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import sun.tools.jconsole.LocalVirtualMachine;

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

    LocalVirtualMachine.getAllVirtualMachines().size();
    int VMcount = LocalVirtualMachine.getAllVirtualMachines().size();

    System.out.println("LocalVirtualMachine.getAllVirtualMachines() = " + LocalVirtualMachine.getAllVirtualMachines());
    
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    ProcessWrapper wrapper = wrapper = new ProcessWrapper(ProcessTest.TestProcess.class.getName(),
      Collections.EMPTY_MAP, "TEST", freePort, runtime.getClassPath());

    assertThat(wrapper).isNotNull();
    assertThat(wrapper.isReady()).isTrue();

    assertThat(LocalVirtualMachine.getAllVirtualMachines().size()).isEqualTo(VMcount + 1);

    wrapper.stop();
    assertThat(LocalVirtualMachine.getAllVirtualMachines().size()).isEqualTo(VMcount);

  }
}