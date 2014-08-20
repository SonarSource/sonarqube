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
import org.elasticsearch.client.Requests;
import org.sonar.server.search.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeleteKey<K extends Serializable> extends IndexActionRequest {

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
  public Class<?> getPayloadClass() {
    throw new IllegalStateException("Deletion by key does not have an object payload!");
  }

  @Override
  public List<ActionRequest> doCall(Index index) throws Exception {
    List<ActionRequest> requests = new ArrayList<ActionRequest>();
    requests.add(Requests.deleteRequest(index.getIndexName())
      .id(getKey())
      .type(indexType));
    return requests;
  }

}
