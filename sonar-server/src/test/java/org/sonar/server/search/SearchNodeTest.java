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

package org.sonar.server.search;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.test.TestUtils;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchNodeTest {

  private ServerFileSystem fileSystem;
  private Settings settings;
  private ImmutableSettings.Builder settingsBuilder;
  private NodeBuilder nodeBuilder;
  private SearchNode searchNode;

  @Before
  public void createMocks() {
    fileSystem = mock(ServerFileSystem.class);
    File tempHome = TestUtils.getTestTempDir(getClass(), "sonarHome");
    when(fileSystem.getHomeDir()).thenReturn(tempHome);

    File tempES = new File(tempHome, "data/es");
    tempES.mkdirs();

    settings = mock(Settings.class);

    settingsBuilder = mock(ImmutableSettings.Builder.class);
    when(settingsBuilder.put(anyString(), anyString())).thenReturn(settingsBuilder);
    when(settingsBuilder.put(anyString(), anyInt())).thenReturn(settingsBuilder);

    nodeBuilder = mock(NodeBuilder.class);
    when(nodeBuilder.local(anyBoolean())).thenReturn(nodeBuilder);
    when(nodeBuilder.clusterName(anyString())).thenReturn(nodeBuilder);
    when(nodeBuilder.data(anyBoolean())).thenReturn(nodeBuilder);
    when(nodeBuilder.settings(settingsBuilder)).thenReturn(nodeBuilder);

    searchNode = new SearchNode(fileSystem, settings, settingsBuilder, nodeBuilder);
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_if_no_data_dir() {
    ServerFileSystem invalidFileSystem = mock(ServerFileSystem.class);
    File tempHome = TestUtils.getTestTempDir(getClass(), "sonarHome");
    when(invalidFileSystem.getHomeDir()).thenReturn(tempHome);
    searchNode = new SearchNode(invalidFileSystem, null, null, null);
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_if_not_properly_started() {
    searchNode.stop();
    searchNode.client();
  }

  @Test
  public void should_manage_node_without_http() {
    Node node = mock(Node.class);
    Client client = mock(Client.class);

    when(nodeBuilder.node()).thenReturn(node);
    when(node.client()).thenReturn(client);

    searchNode.start();
    assertThat(searchNode.client()).isEqualTo(client);
    searchNode.stop();

    verify(settingsBuilder).put("http.enabled", false);
    verify(nodeBuilder).node();
    verify(node).client();
    verify(node).close();
  }

  @Test
  public void should_initialize_node_with_http() {
    String httpHost = "httpHost";
    String httpPort = "httpPort";
    when(settings.getString("sonar.es.http.host")).thenReturn(httpHost);
    when(settings.getString("sonar.es.http.port")).thenReturn(httpPort);

    searchNode.start();

    verify(settingsBuilder).put("http.enabled", true);
    verify(settingsBuilder).put("http.host", httpHost);
    verify(settingsBuilder).put("http.port", httpPort);
  }

  @Test
  public void should_initialize_node_with_http_on_localhost() {
    String httpPort = "httpPort";
    when(settings.getString("sonar.es.http.port")).thenReturn(httpPort);

    searchNode.start();

    verify(settingsBuilder).put("http.enabled", true);
    verify(settingsBuilder).put("http.host", "127.0.0.1");
    verify(settingsBuilder).put("http.port", httpPort);
  }
}
