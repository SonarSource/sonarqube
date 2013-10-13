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

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

public class WebappTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void fail_on_error() throws Exception {
    Env env = mock(Env.class);
    File webDir = temp.newFolder("web");
    when(env.file("web")).thenReturn(webDir);

    Tomcat tomcat = mock(Tomcat.class, RETURNS_DEEP_STUBS);
    when(tomcat.addContext("", webDir.getAbsolutePath())).thenThrow(new NullPointerException());

    try {
      Webapp.configure(tomcat, env, new Props(new Properties()));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to configure webapp");
    }
  }

  @Test
  public void configure_dev_mode() throws Exception {
    Props props = mock(Props.class);
    when(props.booleanOf("sonar.web.dev")).thenReturn(true);
    Context context = mock(Context.class);

    Webapp.configureRailsMode(props, context);

    verify(context).addParameter("jruby.max.runtimes", "3");
    verify(context).addParameter("rails.env", "development");
  }

  @Test
  public void configure_production_mode() throws Exception {
    Props props = mock(Props.class);
    when(props.booleanOf("sonar.web.dev")).thenReturn(false);
    Context context = mock(Context.class);

    Webapp.configureRailsMode(props, context);

    verify(context).addParameter("jruby.max.runtimes", "1");
    verify(context).addParameter("rails.env", "production");
  }

  @Test
  public void context_must_start_with_slash() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.web.context", "foo");

    try {
      Webapp.getContext(new Props(p));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Value of sonar.web.context must start with a forward slash: foo");
    }
  }

  @Test
  public void default_context_is_root() throws Exception {
    String context = Webapp.getContext(new Props(new Properties()));
    assertThat(context).isEqualTo("");
  }
}
