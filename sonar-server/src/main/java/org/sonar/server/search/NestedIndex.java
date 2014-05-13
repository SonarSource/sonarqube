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

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.db.Dto;
import org.sonar.core.profiling.Profiling;

import java.io.Serializable;

public abstract class NestedIndex<D, E extends Dto<K>, K extends Serializable>
  extends BaseIndex<D, E, K> {

  private static final Logger LOG = LoggerFactory.getLogger(NestedIndex.class);

  protected BaseIndex<?,?,?> parentIndex;

  public NestedIndex(IndexDefinition indexDefinition, BaseNormalizer<E, K> normalizer, WorkQueue workQueue,
                     Profiling profiling, BaseIndex<?,?,?> index) {
    super(indexDefinition, normalizer, workQueue, profiling, index.getNode());
    this.parentIndex = index;
  }

  protected Client getClient() {
    return parentIndex.getClient();
  }

  /* Base CRUD methods */

  protected abstract String getParentKeyValue(K key);

  protected abstract String getParentIndexType();

  protected abstract String getIndexField();

  protected String getKeyValue(K key){
    return this.getParentKeyValue(key);
  }

  protected void initializeIndex() {
    ;
  }

  @Override
  public D getByKey(K key) {
    return toDoc( getClient().prepareGet(this.getIndexName(), this.indexDefinition.getIndexType(), this.getKeyValue(key))
      .get());
  }

  @Override
  protected void updateDocument(UpdateRequest request, K key) throws Exception {
    LOG.debug("UPDATE _id:{} in index {}", key, this.getIndexName());
    getClient().update(request
      .index(this.getIndexName())
      .id(this.getKeyValue(key))
      .type(this.getParentIndexType())).get();
  }

}
