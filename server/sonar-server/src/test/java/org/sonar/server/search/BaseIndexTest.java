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
package org.sonar.server.search;


import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.core.cluster.NullQueue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class BaseIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  ESNode node;

  @Before
  public void setup() throws IOException {
    File dataDir = temp.newFolder();
    Settings settings = new Settings();
    settings.setProperty("sonar.path.data", dataDir.getAbsolutePath());
    node = new ESNode(settings);
    node.start();
  }

  @After
  public void tearDown() {
    node.stop();
  }

  @Test
  public void can_load() {
    BaseIndex index = getIndex(this.node);
    assertThat(index).isNotNull();
  }

  @Test
  public void creates_domain_index() {
    BaseIndex index = getIndex(this.node);

    IndicesExistsResponse indexExistsResponse = index.getClient().admin().indices()
      .prepareExists(IndexDefinition.TEST.getIndexName()).execute().actionGet();

    assertThat(indexExistsResponse.isExists()).isTrue();
  }


  private BaseIndex getIndex(final ESNode esNode) {
    BaseIndex index = new BaseIndex(
      IndexDefinition.TEST,
      null, new NullQueue(), esNode) {
      @Override
      protected String getKeyValue(Serializable key) {
        return null;
      }

      @Override
      protected org.elasticsearch.common.settings.Settings getIndexSettings() throws IOException {
        return ImmutableSettings.builder().build();
      }

      @Override
      protected Map mapProperties() {
        return Collections.emptyMap();
      }

      @Override
      protected Map mapKey() {
        return Collections.emptyMap();
      }

      @Override
      public Object toDoc(Map fields) {
        return null;
      }
    };
    index.start();
    return index;
  }
}
