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

import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.db.Dto;
import org.sonar.server.search.Index;

import java.util.List;

public class UpsertDto<DTO extends Dto> extends IndexAction<UpdateRequest> {

  private final DTO dto;

  public UpsertDto(String indexType, DTO dto) {
    this(indexType, dto, true);
  }

  public UpsertDto(String indexType, DTO dto, boolean requiresRefresh) {
    super(indexType, requiresRefresh);
    this.dto = dto;
  }

  @Override
  public String getKey() {
    return dto.getKey().toString();
  }

  @Override
  public List<UpdateRequest> doCall(Index index) {
    List<UpdateRequest> updates = index.getNormalizer().normalize(dto);
    for (UpdateRequest update : updates) {
      update.index(index.getIndexName())
        .type(index.getIndexType())
        .refresh(needsRefresh());
    }
    return updates;
  }
}
