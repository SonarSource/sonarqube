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
import org.sonar.core.db.Dto;

import java.io.Serializable;

public class IndexAction extends QueueAction {

  private static final Logger LOG = LoggerFactory.getLogger(IndexAction.class);


  public enum Method {
    INSERT, UPDATE, DELETE
  }

  private String indexName;
  private Object item;
  private Method method;
  private Index index;


  public IndexAction(String indexName, Method method, Object item){
    super();
    this.indexName = indexName;
    this.method = method;
    this.item = item;
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

  public Object getItem() {
    return item;
  }

  public void setItem(Object item) {
    this.item = item;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  @Override
  public void doExecute() {
    long start = System.currentTimeMillis();
    try {
      if (this.getMethod().equals(Method.DELETE)) {
        index.delete(this.getItem());
      } else if (this.getMethod().equals(Method.INSERT)) {
        index.insert(this.getItem());
      } else if (this.getMethod().equals(Method.UPDATE)) {
        index.update(this.getItem());
      }
      LOG.debug("Action {} in {} took {}ms", this.getMethod(),
        this.getIndexName(), (System.currentTimeMillis() - start));
    } catch (Exception e) {
      LOG.error("Index {} cannot execute {} with {}", this.getIndexName(),
        this.getMethod(), this.getItem().toString());
    } catch (InvalidIndexActionException e) {
      LOG.error("Index {} cannot execute {} with {}", this.getIndexName(),
        this.getMethod(), this.getItem().toString());
    }
  }

  @SuppressWarnings("unchecked")
  public void setIndex(Index index) {
    this.index = index;
  }

  @Override
  public String toString(){
    return "{IndexAction {key: " + getItem()+"}";
  }
}
