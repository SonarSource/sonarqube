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
package org.sonar.server.db;

import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.Dto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

@ServerSide
public interface Dao<DTO extends Dto<KEY>, KEY extends Serializable> {

  /**
   * Get a DTO by its key. Return <code>null</code> if the key does not exist.
   */
  @CheckForNull
  DTO getNullableByKey(DbSession session, KEY key);

  /**
   * Get a DTO by its key.
   *
   * @throws org.sonar.server.exceptions.NotFoundException if the key does not exist
   */
  DTO getByKey(DbSession session, KEY key);

  /**
   * Update a table row. DTO id must be set. The field updatedAt
   * is changed by this method.
   */
  DTO update(DbSession session, DTO dto);

  /**
   * Update one or more table rows. Note that the returned DTO is only
   * the first updated one.
   */
  DTO update(DbSession session, DTO dto, DTO... others);

  Collection<DTO> update(DbSession session, Collection<DTO> dtos);

  DTO insert(DbSession session, DTO dto);

  /**
   * Insert one or more database rows. Note
   * that the returned DTO is only the first inserted one.
   */
  DTO insert(DbSession session, DTO dto, DTO... others);

  Collection<DTO> insert(DbSession session, Collection<DTO> dtos);

  void delete(DbSession session, DTO dto);

  /**
   * Delete one or more table rows.
   */
  void delete(DbSession session, DTO dto, DTO... others);

  void delete(DbSession session, Collection<DTO> dtos);

  void deleteByKey(DbSession session, KEY key);

  void synchronizeAfter(final DbSession session);

  void synchronizeAfter(DbSession session, @Nullable Date date);

  void synchronizeAfter(DbSession session, @Nullable Date date, Map<String, String> params);

}
