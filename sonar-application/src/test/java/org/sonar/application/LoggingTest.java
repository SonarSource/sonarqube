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

import ch.qos.logback.access.tomcat.LogbackValve;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Valve;
import org.apache.catalina.startup.Tomcat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.File;
import java.util.Properties;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setHome() {
    File homeDir = temp.newFolder("home");
    System.setProperty("SONAR_HOME", homeDir.getAbsolutePath());
  }

  @Test
  public void enable_access_logs_by_Default() throws Exception {
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Props props = new Props(new Properties());
    Env env = mock(Env.class);
    when(env.file(Logging.ACCESS_RELATIVE_PATH)).thenReturn(temp.newFile("logback-access.xml"));
    Logging.configure(tomcat, env, props);

    verify(tomcat.getHost().getPipeline()).addValve(argThat(new ArgumentMatcher<Valve>() {
      @Override
      public boolean matches(Object o) {
        LogbackValve v = (LogbackValve) o;
        String confFile = v.getFilename();
        return confFile.endsWith("logback-access.xml");
      }
    }));
  }

  @Test
  public void log_when_started_and_stopped() {
    Logger logger = mock(Logger.class);
    Logging.LifecycleLogger listener = new Logging.LifecycleLogger(logger);

    LifecycleEvent event = new LifecycleEvent(mock(Lifecycle.class), "before_init", null);
    listener.lifecycleEvent(event);
    verifyZeroInteractions(logger);

    event = new LifecycleEvent(mock(Lifecycle.class), "after_start", null);
    listener.lifecycleEvent(event);
    verify(logger).info("Web server is started");

    event = new LifecycleEvent(mock(Lifecycle.class), "after_destroy", null);
    listener.lifecycleEvent(event);
    verify(logger).info("Web server is stopped");
  }
}
