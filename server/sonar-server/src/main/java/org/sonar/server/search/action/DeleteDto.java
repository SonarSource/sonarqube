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
import org.sonar.core.persistence.Dto;
import org.sonar.server.search.Index;

import java.util.ArrayList;
import java.util.List;

public class DeleteDto<DTO extends Dto> extends IndexActionRequest {

  private final DTO dto;

  public DeleteDto(String indexType, DTO dto) {
    super(indexType);
    this.dto = dto;
  }

  @Override
  public String getKey() {
    return dto.getKey().toString();
  }

  @Override
  public Class<?> getPayloadClass() {
    return dto.getClass();
  }

  @Override
  public List<ActionRequest> doCall(Index index) throws Exception {
    List<ActionRequest> requests = new ArrayList<ActionRequest>();
    requests.add(Requests.deleteRequest(index.getIndexName())
      .id(dto.getKey().toString())
      .type(indexType));
    return requests;
  }
}
