/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.Monitored;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Ignore
// FIXME enable back right now!
public class SearchServerTest {

  private static final String A_CLUSTER_NAME = "a_cluster";
  private static final String A_NODE_NAME = "a_node";

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private int httpPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
  private int port = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
  private SearchServer underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.stop();
      underTest.awaitStop();
    }
  }

  @Test
  public void start_stop_server() throws Exception {
    underTest = new SearchServer(getClusterProperties());

    underTest.start();
    assertThat(underTest.getStatus()).isEqualTo(Monitored.Status.OPERATIONAL);

    underTest.stop();
    underTest.awaitStop();
    try {
      underTest.getStatus();
      fail();
    } catch (Exception exception) {
      // ok
    }
  }

  private void waitFor(String expectedStatus) throws Exception {
    String urlString = "http://localhost:" + httpPort + "/_cluster/health";
    URL url = new URL(urlString);
    URLConnection urlConnection = url.openConnection();
    InputStream inputStream = urlConnection.getInputStream();
    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    while ((line = reader.readLine()) != null) {
      if (line.contains("\"status\"")) {
        if (line.contains(expectedStatus)) {
          return;
        }
      }
    }
    fail();
  }

  private Props getClusterProperties() throws IOException {
    Props props = new Props(new Properties());
    // the following properties have always default values (see ProcessProperties)
    InetAddress host = InetAddress.getLoopbackAddress();
    props.set(ProcessProperties.SEARCH_HOST, host.getHostAddress());
    props.set(ProcessProperties.SEARCH_HTTP_PORT, String.valueOf(httpPort));
    props.set(ProcessProperties.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessProperties.CLUSTER_NAME, A_CLUSTER_NAME);
    props.set(EsSettings.CLUSTER_SEARCH_NODE_NAME, A_NODE_NAME);
    props.set(ProcessProperties.PATH_HOME, temp.newFolder().getAbsolutePath());
    props.set(ProcessEntryPoint.PROPERTY_SHARED_PATH, temp.newFolder().getAbsolutePath());
    return props;
  }
}
