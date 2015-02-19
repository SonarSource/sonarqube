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
package org.sonar.server.app;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.NetworkUtils;
import org.sonar.process.Props;

import java.io.File;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.URL;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class EmbeddedTomcatTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void start() throws Exception {
    Props props = new Props(new Properties());

    // prepare file system
    File home = temp.newFolder();
    File webDir = new File(home, "web");
    FileUtils.write(new File(home, "web/WEB-INF/web.xml"), "<web-app/>");
    props.set("sonar.path.home", home.getAbsolutePath());
    props.set("sonar.path.web", webDir.getAbsolutePath());
    props.set("sonar.path.logs", temp.newFolder().getAbsolutePath());

    // start server on a random port
    int httpPort = NetworkUtils.freePort();
    int ajpPort = NetworkUtils.freePort();
    props.set("sonar.web.port", String.valueOf(httpPort));
    props.set("sonar.ajp.port", String.valueOf(ajpPort));
    EmbeddedTomcat tomcat = new EmbeddedTomcat(props);
    assertThat(tomcat.isReady()).isFalse();
    tomcat.start();
    assertThat(tomcat.isReady()).isTrue();

    // check that http connector accepts requests
    URL url = new URL("http://" + Inet4Address.getLocalHost().getHostAddress() + ":" + httpPort);
    url.openConnection().connect();

    // stop server
    tomcat.terminate();
    // tomcat.isReady() must not be called. It is used to wait for server startup, not shutdown.
    try {
      url.openConnection().connect();
      fail();
    } catch (ConnectException e) {
      // expected
    }
  }
}
