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

import org.picocontainer.Startable;
import org.sonar.core.db.Dto;

import javax.annotation.CheckForNull;

import java.io.Serializable;

public interface Index<E extends Dto<K>, K extends Serializable> extends Startable {

  String getIndexName();

  @CheckForNull
  Hit getByKey(K item);

  void refresh();

  void insert(Object obj) throws InvalidIndexActionException;

  void insertByKey(K key);

  void insertByDto(E item);

  void update(Object obj) throws InvalidIndexActionException;

  void updateByKey(K key);

  void updateByDto(E item);

  void delete(Object obj) throws InvalidIndexActionException;

  void deleteByKey(K key);

  void deleteByDto(E item);

  Long getLastSynchronization();

  void setLastSynchronization(Long time);


}
