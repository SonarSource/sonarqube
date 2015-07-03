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
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.db.Dto;
import org.sonar.server.search.Index;

import java.util.ArrayList;
import java.util.List;

public class InsertDto<DTO extends Dto> extends IndexAction<ActionRequest> {

  private final DTO dto;

  public InsertDto(String indexType, DTO dto, boolean requiresRefresh) {
    super(indexType, requiresRefresh);
    this.dto = dto;
  }

  @Override
  public String getKey() {
    return dto.getKey().toString();
  }

  @Override
  public List<ActionRequest> doCall(Index index) {
    List<ActionRequest> inserts = new ArrayList<>();
    List<UpdateRequest> updates = index.getNormalizer().normalize(dto);
    for (UpdateRequest update : updates) {
      if (update.doc() != null) {
        inserts.add(update.upsertRequest()
          .index(index.getIndexName())
          .type(index.getIndexType())
          .id(update.id())
          .routing(update.routing()));
      } else {
        inserts.add(update
          .index(index.getIndexName())
          .type(index.getIndexType()));
      }
    }
    return inserts;
  }
}
