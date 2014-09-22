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
package org.sonar.server.search.action;

import org.elasticsearch.action.ActionRequest;
import org.sonar.server.search.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeleteNestedItem<K extends Serializable> extends IndexActionRequest {

  private final K key;
  private final Object item;
  private final Object[] items;

  public DeleteNestedItem(String indexType, K key, Object item, Object... items) {
    super(indexType);
    this.key = key;
    this.item = item;
    this.items = items;
  }

  @Override
  public String getKey() {
    return this.key.toString();
  }

  @Override
  public Class getPayloadClass() {
    return item.getClass();
  }

  @Override
  public List<ActionRequest> doCall(Index index) throws Exception {
    List<ActionRequest> updates = new ArrayList<ActionRequest>();
    updates.addAll(deleteItem(index, item, key));
    for (Object otherItem : items) {
      updates.addAll(deleteItem(index, otherItem, key));
    }
    return updates;
  }

  private List<ActionRequest> deleteItem(Index index, Object item, K key) {
    return index.getNormalizer().deleteNested(item, key);
  }
}
