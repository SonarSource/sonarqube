/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebappTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Props props = new Props(new Properties());

  @Before
  public void initDataDir() throws Exception {
    props.set(ProcessProperties.PATH_DATA, temp.newFolder("data").getAbsolutePath());
  }

  @Test
  public void fail_on_error() throws Exception {
    File webDir = temp.newFolder("web");

    Tomcat tomcat = mock(Tomcat.class, RETURNS_DEEP_STUBS);
    when(tomcat.addContext("", webDir.getAbsolutePath())).thenThrow(new NullPointerException());

    try {
      Webapp.configure(tomcat, props);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to configure webapp");
    }
  }

  @Test
  public void configure_context() throws Exception {
    props.set("foo", "bar");
    StandardContext context = mock(StandardContext.class);
    Tomcat tomcat = mock(Tomcat.class);
    when(tomcat.addWebapp(anyString(), anyString())).thenReturn(context);

    Webapp.configure(tomcat, props);

    // configure webapp with properties
    verify(context).addParameter("foo", "bar");
  }

  @Test
  public void configure_rails_dev_mode() {
    props.set("sonar.web.dev", "true");
    Context context = mock(Context.class);

    Webapp.configureRails(props, context);

    verify(context).addParameter("jruby.max.runtimes", "3");
    verify(context).addParameter("rails.env", "development");
  }

  @Test
  public void configure_production_mode() {
    props.set("sonar.web.dev", "false");
    Context context = mock(Context.class);

    Webapp.configureRails(props, context);

    verify(context).addParameter("jruby.max.runtimes", "1");
    verify(context).addParameter("rails.env", "production");
  }
}
