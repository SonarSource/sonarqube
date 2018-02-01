/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.app;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.startup.Tomcat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.utils.log.Logger;
import org.sonar.process.Props;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

public class TomcatAccessLogTest {

  TomcatAccessLog underTest = new TomcatAccessLog();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setHome() throws IOException {
    File homeDir = temp.newFolder("home");
    System.setProperty("SONAR_HOME", homeDir.getAbsolutePath());
  }

  @Test
  public void enable_access_logs_by_Default() throws Exception {
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Props props = new Props(new Properties());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    underTest.configure(tomcat, props);

    verify(tomcat.getHost().getPipeline()).addValve(any(ProgrammaticLogbackValve.class));
  }

  @Test
  public void log_when_started_and_stopped() {
    Logger logger = mock(Logger.class);
    TomcatAccessLog.LifecycleLogger listener = new TomcatAccessLog.LifecycleLogger(logger);

    LifecycleEvent event = new LifecycleEvent(mock(Lifecycle.class), "before_init", null);
    listener.lifecycleEvent(event);
    verifyZeroInteractions(logger);

    event = new LifecycleEvent(mock(Lifecycle.class), "after_start", null);
    listener.lifecycleEvent(event);
    verify(logger).debug("Tomcat is started");

    event = new LifecycleEvent(mock(Lifecycle.class), "after_destroy", null);
    listener.lifecycleEvent(event);
    verify(logger).debug("Tomcat is stopped");
  }
}
