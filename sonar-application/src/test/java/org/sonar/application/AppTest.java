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
package org.sonar.application;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Process;
import org.sonar.process.ProcessMXBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AppTest {

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
  @Ignore
  public void should_register_mbean() throws Exception {
    Installation installation = mock(Installation.class);
    when(installation.detectHomeDir()).thenReturn(sonarHome.getRoot());

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    App server = new App(installation);

    // 0 Can have a valid ObjectName
    assertThat(server).isNotNull();

    // 1 assert that process MBean is registered
    ObjectName serverObjectName = Process.objectNameFor(App.PROCESS_NAME);
    assertThat(mbeanServer.isRegistered(serverObjectName)).isTrue();

    // 2 assert that we can remotely call ping
    Long now = System.currentTimeMillis();
    Long ping = (Long) mbeanServer.invoke(serverObjectName, ProcessMXBean.PING, null, null);
    assertThat(ping).isNotNull();
    assertThat(ping - now).isLessThanOrEqualTo(3000L);

    // 3 assert that we can remotely call isReady
    //TODO this method is for some reason not available...
//    Boolean isReady = (Boolean) mbeanServer.invoke(serverObjectName, ProcessMXBean.IS_READY, null, null);
//    assertThat(isReady).isFalse();

    // 4 assert that we can remotely call terminate
    mbeanServer.invoke(serverObjectName, ProcessMXBean.TERMINATE, null, null);

    // 5 assert that we can remotely call terminate
    try {
      mbeanServer.invoke(serverObjectName, "xoxo", null, null);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("No such operation: xoxo");
    }
  }
}
