/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.apache.catalina.Valve;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class LoggingTest {
  @Test
  public void configure_access_logs() throws Exception {
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Env env = mock(Env.class);
    final File propsFile = new File(getClass().getResource("/org/sonar/application/LoggingTest/logback-access.xml").toURI());
    when(env.file(Logging.CONF_PATH)).thenReturn(propsFile);
    Logging.configure(tomcat, env);

    verify(tomcat.getHost().getPipeline()).addValve(argThat(new ArgumentMatcher<Valve>() {
      @Override
      public boolean matches(Object o) {
        LogbackValve v = (LogbackValve) o;
        return v.getFilename().equals(propsFile.getAbsolutePath());
      }
    }));
  }

  @Test
  public void fail_if_missing_conf_file() throws Exception {
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    Env env = mock(Env.class);
    final File confFile = new File("target/does_not_exist/logback-access.xml");
    when(env.file(Logging.CONF_PATH)).thenReturn(confFile);

    try {
      Logging.configure(tomcat, env);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("File is missing: " + confFile.getAbsolutePath());
    }
  }
}
