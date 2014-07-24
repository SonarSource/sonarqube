package org.sonar.application;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Process;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StartServerTest {

  @Rule
  public TemporaryFolder sonarHome = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    sonarHome.create();
    FileUtils.copyURLToFile(this.getClass().getClassLoader().getResource("conf/"),
      new File(sonarHome.getRoot().getAbsolutePath(), "conf"));
  }

  @After
  public void tearDown() throws Exception {
    sonarHome.delete();
  }

  @Test
  public void should_register_mbean() throws Exception {

    Installation installation = mock(Installation.class);
    when(installation.detectHomeDir()).thenReturn(sonarHome.getRoot());

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    StartServer server = new StartServer(installation);

    // 0 Can have a valid ObjectName
    assertThat(server).isNotNull();

    // 1 assert that process MBean is registered
    ObjectName serverObjectName = Process.objectNameFor(StartServer.PROCESS_NAME);
    assertThat(mbeanServer.isRegistered(serverObjectName)).isTrue();

    ObjectInstance serverMXBean = mbeanServer.getObjectInstance(serverObjectName);


    System.out.println("serverMXBean.getClassName() = " + serverMXBean.getClassName());

//    Class<?> processClass = serverMXBean.getClassName();
//
//    Method method =
//    Reflection.invoke(serverMXBean, "ping", Long.class);
//
//    // 2 assert that we cannot make another Process in the same JVM
//    try {
//      process = new TestProcess(props);
//      fail();
//    } catch (IllegalStateException e) {
//      assertThat(e.getMessage()).isEqualTo("Process already exists in current JVM");
//    }
  }
}