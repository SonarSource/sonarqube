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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Create registered indices in Elasticsearch.
 */
public class IndexCreator implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexCreator.class);

  /**
   * Internal setting stored on index to know its version. It's used to re-create index
   * when something changed between versions.
   */
  private static final String SETTING_HASH = "sonar_hash";

  private final EsClient client;
  private final IndexDefinition[] definitions;

  public IndexCreator(EsClient client, IndexDefinition[] definitions) {
    this.client = client;
    this.definitions = definitions;
  }

  @Override
  public void start() {
    create();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public void create() {
    // collect definitions
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();
    for (IndexDefinition definition : definitions) {
      definition.define(context);
    }

    // create indices that do not exist or that have a new definition (different mapping, cluster enabled, ...)
    for (NewIndex newIndex : context.getIndices().values()) {
      boolean exists = client.prepareExists(newIndex.getName()).get().isExists();
      if (exists) {
        if (needsToDeleteIndex(newIndex)) {
          LOGGER.info(String.format("Delete index %s (settings changed)", newIndex.getName()));
          deleteIndex(newIndex.getName());
          exists = false;
        }
      }
      if (!exists) {
        createIndex(newIndex);
      }
    }
  }

  private void createIndex(NewIndex newIndex) {
    LOGGER.info(String.format("Create index %s", newIndex.getName()));
    ImmutableSettings.Builder settings = newIndex.getSettings();
    settings.put(SETTING_HASH, new IndexHash().of(newIndex));
    client
      .prepareCreate(newIndex.getName())
      .setSettings(settings)
      .get();

    // create types
    for (Map.Entry<String, NewIndex.NewMapping> entry : newIndex.getMappings().entrySet()) {
      LOGGER.info(String.format("Create type %s/%s", newIndex.getName(), entry.getKey()));
      client.preparePutMapping(newIndex.getName())
        .setType(entry.getKey())
        .setIgnoreConflicts(false)
        .setSource(entry.getValue().getAttributes())
        .get();
    }
  }

  private void deleteIndex(String indexName) {
    client.nativeClient().admin().indices().prepareDelete(indexName).get();
  }

  private boolean needsToDeleteIndex(NewIndex index) {
    boolean toBeDeleted = false;
    String hash = client.nativeClient().admin().indices().prepareGetSettings(index.getName()).get().getSetting(index.getName(), "index." + SETTING_HASH);
    if (hash != null) {
      String defHash = new IndexHash().of(index);
      toBeDeleted = !StringUtils.equals(hash, defHash);
    }
    return toBeDeleted;
  }
}
