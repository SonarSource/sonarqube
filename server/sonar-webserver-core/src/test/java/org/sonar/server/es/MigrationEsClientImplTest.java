/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

class MigrationEsClientImplTest {
  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @RegisterExtension
  public EsTester es = EsTester.createCustom(
    new SimpleIndexDefinition("as"),
    new SimpleIndexDefinition("bs"),
    new SimpleIndexDefinition("cs"));

  private final MigrationEsClient underTest = new MigrationEsClientImpl(es.client());

  @Test
  void delete_existing_index() {
    underTest.deleteIndexes("as");

    assertThat(loadExistingIndices())
      .toIterable()
      .doesNotContain("as")
      .contains("bs", "cs");
    assertThat(logTester.logs(Level.INFO))
      .contains("Drop Elasticsearch indices [as]");
  }

  @Test
  void delete_index_that_does_not_exist() {
    underTest.deleteIndexes("as", "xxx", "cs");

    assertThat(loadExistingIndices())
      .toIterable()
      .doesNotContain("as", "cs")
      .contains("bs");
    assertThat(logTester.logs(Level.INFO))
      .contains("Drop Elasticsearch indices [as,cs]")
      .doesNotContain("Drop Elasticsearch indices [xxx]");
  }

  private Iterator<String> loadExistingIndices() {
    return es.client().getMapping(new GetMappingsRequest()).mappings().keySet().iterator();
  }

  private static class SimpleIndexDefinition implements IndexDefinition {
    private final String indexName;

    public SimpleIndexDefinition(String indexName) {
      this.indexName = indexName;
    }

    @Override
    public void define(IndexDefinitionContext context) {
      IndexType.IndexMainType mainType = IndexType.main(Index.simple(indexName), indexName.substring(1));
      context.create(
        mainType.getIndex(),
        newBuilder(new MapSettings().asConfig()).build())
        .createTypeMapping(mainType);
    }
  }
}
