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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Dto;

import javax.annotation.CheckForNull;
import java.io.Serializable;

public interface Index<D, E extends Dto<K>, K extends Serializable> extends Startable, ServerComponent {

  @CheckForNull
  D getByKey(K item);

  SearchResponse search(SearchRequestBuilder request,
                        FilterBuilder filter, QueryBuilder query);

  String getIndexType();

  String getIndexName();

  void refresh();

  void insert(Object obj, K key) throws Exception;

  void insertByKey(K key);

  void insertByDto(E dto);

  void update(Object obj, K key) throws Exception;

  void updateByKey(K key);

  void updateByDto(E dto);

  void delete(Object obj, K key) throws Exception;

  void deleteByKey(K key);

  void deleteByDto(E dto);

  Long getLastSynchronization();

  void setLastSynchronization(Long time);


}
