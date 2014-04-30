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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.QueueAction;

import java.io.Serializable;

public class IndexAction<K extends Serializable> extends QueueAction {

  private static final Logger LOG = LoggerFactory.getLogger(IndexAction.class);


  public enum Method {
    INSERT, UPDATE, DELETE
  }

  private String indexName;
  private K key;
  private Method method;
  private Index<K> index;


  public IndexAction(String indexName, Method method, K key){
    super();
    this.indexName = indexName;
    this.method = method;
    this.key = key;
  }

  public Method getMethod(){
    return this.method;
  }

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  public K getKey() {
    return key;
  }

  public void setKey(K key) {
    this.key = key;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  @Override
  public void doExecute() {
    long start = System.currentTimeMillis();
    if (this.getMethod().equals(Method.DELETE)) {
      index.delete(this.getKey());
    } else if (this.getMethod().equals(Method.INSERT)) {
      index.insert(this.getKey());
    } else if (this.getMethod().equals(Method.UPDATE)) {
      index.update(this.getKey());
    }
    //TODO execute ACtion when DTO available
    LOG.debug("Action {} in {} took {}ms", this.getMethod(),
      this.getIndexName(), (System.currentTimeMillis() - start));
  }

  @SuppressWarnings("unchecked")
  public void setIndex(Index<?> index) {
    this.index = (Index<K>) index;
  }

  @Override
  public String toString(){
    return "{IndexAction {key: " + getKey()+"}";
  }
}
