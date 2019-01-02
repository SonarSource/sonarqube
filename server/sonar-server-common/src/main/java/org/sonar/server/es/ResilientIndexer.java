/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.es;

import java.util.Collection;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

/**
 * This kind of indexers that are resilient
 */
public interface ResilientIndexer extends StartupIndexer {

  /**
   * Index the items and delete them from es_queue table when the indexation
   * is done, keep it if there is a failure on the item of the collection
   *
   * @param dbSession the db session
   * @param items     the items to be indexed
   * @return the number of successful indexation
   */
  IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items);
}
