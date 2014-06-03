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
package org.sonar.server.log.index;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.log.Log;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.log.db.LogKey;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.ESNode;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.4
 */
public class LogIndex extends BaseIndex<Log, LogDto, LogKey>{

  public LogIndex(LogNormalizer normalizer, WorkQueue workQueue, ESNode node) {
    super(IndexDefinition.LOG, normalizer, workQueue, node);
  }

  @Override
  protected String getKeyValue(LogKey key) {
    return key.toString();
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder().build();
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : LogNormalizer.LogFields.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  @Override
  protected Map mapKey() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("path", LogNormalizer.LogFields.KEY.field());
    return mapping;
  }

  @Override
  protected Log toDoc(final Map<String, Object> fields) {
    return new LogDoc(fields);
  }
}
