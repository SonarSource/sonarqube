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

import com.google.common.collect.Lists;
import org.apache.commons.lang.math.RandomUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.ExternalResource;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.core.profiling.Profiling;

import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class EsTester extends ExternalResource {

  private static int PROCESS_ID = RandomUtils.nextInt(Integer.MAX_VALUE);
  private Node node;
  private EsClient client;
  private final List<IndexDefinition> definitions = Lists.newArrayList();

  public EsTester addDefinitions(IndexDefinition... defs) {
    Collections.addAll(definitions, defs);
    return this;
  }

  protected void before() throws Throwable {
    String nodeName = "es-ram-" + PROCESS_ID;
    node = NodeBuilder.nodeBuilder().local(true).data(true).settings(ImmutableSettings.builder()
      .put("cluster.name", nodeName)
      .put("node.name", nodeName)
      // the two following properties are probably not used because they are
      // declared on indices too
      .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
      .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
      // limit the number of threads created (see org.elasticsearch.common.util.concurrent.EsExecutors)
      .put("processors", 1)
      .put("http.enabled", false)
      .put("index.store.type", "memory")
      .put("config.ignore_system_properties", true)
      // reuse the same directory than other tests for faster initialization
      .put("path.home", "target/es-" + PROCESS_ID)
      .put("gateway.type", "none"))
      .build();
    node.start();
    assertThat(DiscoveryNode.localNode(node.settings())).isTrue();

    // delete the indices created by previous tests
    DeleteIndexResponse response = node.client().admin().indices().prepareDelete("_all").get();
    assertThat(response.isAcknowledged()).isTrue();

    client = new EsClient(new Profiling(new Settings()), node.client());
    client.start();

    if (!definitions.isEmpty()) {
      ComponentContainer container = new ComponentContainer();
      container.addSingletons(definitions);
      container.addSingleton(client);
      container.addSingleton(IndexRegistry.class);
      container.addSingleton(IndexCreator.class);
      container.startComponents();
    }
  }

  @Override
  protected void after() {
    if (client != null) {
      client.stop();
    }
    if (node != null) {
      node.stop();
      node.close();
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
    return node;
  }

  public EsClient client() {
    return client;
  }
}
