/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.Props;

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
    File data = temp.newFolder();
    File webDir = new File(home, "web");
    FileUtils.write(new File(home, "web/WEB-INF/web.xml"), "<web-app/>");
    props.set("sonar.path.home", home.getAbsolutePath());
    props.set("sonar.path.data", data.getAbsolutePath());
    props.set("sonar.path.web", webDir.getAbsolutePath());
    props.set("sonar.path.logs", temp.newFolder().getAbsolutePath());

    // start server on a random port
    InetAddress address = InetAddress.getLoopbackAddress();
    int httpPort = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(address);
    props.set("sonar.web.host", address.getHostAddress());
    props.set("sonar.web.port", String.valueOf(httpPort));
    EmbeddedTomcat tomcat = new EmbeddedTomcat(props);
    assertThat(tomcat.getStatus()).isEqualTo(EmbeddedTomcat.Status.DOWN);
    tomcat.start();
    assertThat(tomcat.getStatus()).isEqualTo(EmbeddedTomcat.Status.UP);

    // check that http connector accepts requests
    URL url = new URL("http://" + address.getHostAddress() + ":" + httpPort);
    url.openConnection().connect();

    // stop server
    tomcat.terminate();
    // tomcat.isUp() must not be called. It is used to wait for server startup, not shutdown.
    try {
      url.openConnection().connect();
      fail();
    } catch (ConnectException e) {
      // expected
    }
  }
}
