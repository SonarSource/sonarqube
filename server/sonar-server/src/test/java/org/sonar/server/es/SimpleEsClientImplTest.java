/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.es;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

public class SimpleEsClientImplTest {
  @Rule
  public EsTester es = EsTester.createCustom(new FakeIndexDefinition(),
    new SimpleIndexDefinition("a"),
    new SimpleIndexDefinition("b"),
    new SimpleIndexDefinition("c"));

  private Client client = es.client().nativeClient();
  private SimpleEsClientImpl underTest = new SimpleEsClientImpl(client);

  @Test
  public void call_without_arguments_does_not_generate_an_elasticsearch_call() {
    Client client = mock(Client.class);
    SimpleEsClientImpl underTest = new SimpleEsClientImpl(client);
    underTest.deleteIndexes();

    verify(client, never()).admin();
  }

  @Test
  public void delete_known_indice_must_delete_the_index() {
    underTest.deleteIndexes("fakes");

    assertThat(es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings().get("fakes")).isNull();
  }

  @Test
  public void delete_unknown_indice_must_delete_all_existing_indexes() {
    underTest.deleteIndexes("a", "xxx", "c");

    assertThat(es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings().get("a")).isNull();
    assertThat(es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings().get("c")).isNull();
  }

  public class SimpleIndexDefinition implements IndexDefinition {
    private final String indexName;

    public SimpleIndexDefinition(String indexName) {
      this.indexName = indexName;
    }

    @Override
    public void define(IndexDefinitionContext context) {
      NewIndex index = context.create(indexName, newBuilder(new MapSettings().asConfig()).build());
      index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0);
      index.getSettings().put("index.refresh_interval", "-1");
    }
  }
}
