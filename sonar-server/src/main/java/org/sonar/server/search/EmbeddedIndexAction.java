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

import java.io.Serializable;

/**
 * Created by gamars on 02/05/14.
 * @since
 */
public class EmbeddedIndexAction<K extends Serializable> extends IndexAction {

  private final Object item;
  private final K key;

  public EmbeddedIndexAction(String indexName, Method method, Object item, K key){
    super(indexName, method);
    this.indexName = indexName;
    this.method = method;
    this.key = key;
    this.item = item;
  }

  @Override
  public void doExecute() {
    try {
      if (this.getMethod().equals(Method.DELETE)) {
        index.delete(this.item, this.key);
      } else if (this.getMethod().equals(Method.INSERT)) {
        index.insert(this.item, this.key);
      } else if (this.getMethod().equals(Method.UPDATE)) {
        index.update(this.item, this.key);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Index " + this.getIndexName() + " cannot execute " +
        this.getMethod() + " for " +this.item.getClass().getSimpleName() +
        " on key: "+ this.key, e);
    }
  }
}
