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

import java.util.Iterator;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

public class MigrationEsClientImplTest {
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public EsTester es = EsTester.createCustom(
    new SimpleIndexDefinition("a"),
    new SimpleIndexDefinition("b"),
    new SimpleIndexDefinition("c"));

  private MigrationEsClient underTest = new MigrationEsClientImpl(es.client());

  @Test
  public void delete_existing_index() {
    underTest.deleteIndexes("a");

    assertThat(loadExistingIndices())
      .doesNotContain("a")
      .contains("b", "c");
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("Drop Elasticsearch index [a]");
  }

  @Test
  public void ignore_indices_that_do_not_exist() {
    underTest.deleteIndexes("a", "xxx", "c");

    assertThat(loadExistingIndices())
      .doesNotContain("a", "c")
      .contains("b");
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("Drop Elasticsearch index [a]", "Drop Elasticsearch index [c]")
      .doesNotContain("Drop Elasticsearch index [xxx]");
  }

  private Iterator<String> loadExistingIndices() {
    return es.client().nativeClient().admin().indices().prepareGetMappings().get().mappings().keysIt();
  }

  private static class SimpleIndexDefinition implements IndexDefinition {
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
