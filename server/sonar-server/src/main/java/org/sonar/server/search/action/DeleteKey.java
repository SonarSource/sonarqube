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

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.Requests;
import org.sonar.server.search.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeleteKey<K extends Serializable> extends IndexAction<DeleteRequest> {

  private final K key;

  public DeleteKey(String indexType, K key) {
    super(indexType);
    this.key = key;
  }

  @Override
  public String getKey() {
    return key.toString();
  }

  @Override
  public List<DeleteRequest> doCall(Index index) {
    List<DeleteRequest> requests = new ArrayList<>();
    requests.add(Requests.deleteRequest(index.getIndexName())
      .id(getKey())
      .type(indexType)
      .refresh(needsRefresh()));
    return requests;
  }

}
