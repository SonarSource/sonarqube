package org.sonar.process;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public abstract class ProcessTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public static final String DUMMY_OK_APP = "org.sonar.application.DummyOkProcess";

  int freePort;
  File dummyAppJar;
  Process proc;

  @Before
  public void setup() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    freePort = socket.getLocalPort();
    socket.close();

    dummyAppJar = FileUtils.toFile(getClass().getResource("/sonar-dummy-app.jar"));
  }


  @After
  public void tearDown() {
    if (proc != null) {
      proc.destroy();
    }
  }

}
