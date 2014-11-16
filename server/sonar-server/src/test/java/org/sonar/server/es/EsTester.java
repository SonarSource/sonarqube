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
package org.sonar.server.es;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.ExternalResource;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class EsTester extends ExternalResource {

  /**
   * This instance is shared for performance reasons. Never stopped.
   */
  private static Node sharedNode;
  private static EsClient client;

  @Override
  protected void before() throws Throwable {
    if (sharedNode == null) {
      String nodeName = EsTester.class.getName();
      sharedNode = NodeBuilder.nodeBuilder().local(true).data(true).settings(ImmutableSettings.builder()
        .put(ClusterName.SETTING, nodeName)
        .put("node.name", nodeName)
        // the two following properties are probably not used because they are
        // declared on indices too
        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
        // limit the number of threads created (see org.elasticsearch.common.util.concurrent.EsExecutors)
        .put("processors", 1)
        .put("http.enabled", false)
        .put("index.store.type", "ram")
        .put("config.ignore_system_properties", true)
        .put("gateway.type", "none"))
        .build();
      sharedNode.start();
      assertThat(DiscoveryNode.localNode(sharedNode.settings())).isTrue();
      client = new EsClient(new Profiling(new Settings()), sharedNode.client());

    } else {
      // delete the indices created by previous tests
      DeleteIndexResponse response = sharedNode.client().admin().indices().prepareDelete("_all").get();
      assertThat(response.isAcknowledged()).isTrue();
    }
  }

  public void truncateIndices() {
    client.prepareDeleteByQuery(client.prepareState().get()
      .getState().getMetaData().concreteAllIndices())
      .setQuery(QueryBuilders.matchAllQuery())
      .get();
    client.prepareRefresh(client.prepareState().get()
      .getState().getMetaData().concreteAllIndices())
      .setForce(true)
      .get();
    client.prepareFlush(client.prepareState().get()
      .getState().getMetaData().concreteAllIndices())
      .get();
  }

  public Node node() {
    return sharedNode;
  }

  public EsClient client() {
    return client;
  }
}
