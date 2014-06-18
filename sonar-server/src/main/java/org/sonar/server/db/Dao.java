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

import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.Dto;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

public interface Dao<E extends Dto<K>, K extends Serializable> extends ServerComponent {

  /**
   * Get a DTO by its key. Return <code>null</code> if the key does not exist.
   */
  @CheckForNull
  E getNullableByKey(DbSession session, K key);

  /**
   * Get a DTO by its key.
   *
   * @throws org.sonar.server.exceptions.NotFoundException if the key does not exist
   */
  E getByKey(DbSession session, K key);

  /**
   * Update a table row. DTO id must be set. The field updatedAt
   * is changed by this method.
   */
  E update(DbSession session, E dto);

  /**
   * Update one or more table rows. Note that the returned DTO is only
   * the first updated one.
   */
  E update(DbSession session, E dto, E... others);

  Collection<E> update(DbSession session, Collection<E> dtos);

  E insert(DbSession session, E dto);

  /**
   * Insert one or more database rows. Note
   * that the returned DTO is only the first inserted one.
   */
  E insert(DbSession session, E dto, E... others);

  Collection<E> insert(DbSession session, Collection<E> dtos);

  void delete(DbSession session, E dto);

  /**
   * Delete one or more table rows.
   */
  void delete(DbSession session, E dto, E... others);

  void delete(DbSession session, Collection<E> dtos);

  void deleteByKey(DbSession session, K key);

  void synchronizeAfter(DbSession session, Date date);

}
