/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

/**
 * Clean-up the db table es_queue when documents
 * are successfully indexed so that the recovery
 * daemon does not re-index them.
 */
public class ResiliencyIndexingListener implements IndexingListener {

  private final DbClient dbClient;
  private final DbSession dbSession;
  private final Collection<EsQueueDto> items;

  public ResiliencyIndexingListener(DbClient dbClient, DbSession dbSession, Collection<EsQueueDto> items) {
    this.dbClient = dbClient;
    this.dbSession = dbSession;
    this.items = items;
  }

  @Override
  public void onSuccess(Collection<String> docIds) {
    if (!docIds.isEmpty()) {
      Multimap<String, EsQueueDto> itemsById = items.stream().collect(MoreCollectors.index(EsQueueDto::getDocId, Function.identity()));

      Collection<EsQueueDto> itemsToDelete = docIds
        .stream()
        .map(itemsById::get)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .collect(MoreCollectors.toArrayList(docIds.size()));
      dbClient.esQueueDao().delete(dbSession, itemsToDelete);
      dbSession.commit();
    }
  }
}
