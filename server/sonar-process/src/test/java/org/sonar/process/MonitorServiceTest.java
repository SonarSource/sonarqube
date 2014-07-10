package org.sonar.process;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.DatagramSocket;

public class MonitorServiceTest {


  private DatagramSocket socket;

  @Before
  public void setUp() throws Exception {
    socket = new DatagramSocket(0);
  }

  @After
  public void tearDown() throws Exception {
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
  }

  @Test
  public void should_build() {
    MonitorService monitor = new MonitorService(socket);
  }

  class LongProcessWrapper extends ProcessWrapper {

    LongProcessWrapper(String name, Integer port) {
      super(name, port);
    }

    @Override
    public void run() {
      try {
        Thread.sleep(10000L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}