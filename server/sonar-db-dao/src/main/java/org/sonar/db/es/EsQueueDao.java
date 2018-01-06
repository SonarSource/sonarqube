/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.es;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class EsQueueDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public EsQueueDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public EsQueueDto insert(DbSession dbSession, EsQueueDto item) {
    insert(dbSession, singletonList(item));
    return item;
  }

  public Collection<EsQueueDto> insert(DbSession dbSession, Collection<EsQueueDto> items) {
    long now = system2.now();
    EsQueueMapper mapper = mapper(dbSession);
    items.forEach(item -> {
      item.setUuid(uuidFactory.create());
      mapper.insert(item, now);
    });
    return items;
  }

  public void delete(DbSession dbSession, EsQueueDto item) {
    delete(dbSession, singletonList(item));
  }

  public void delete(DbSession dbSession, Collection<EsQueueDto> items) {
    EsQueueMapper mapper = mapper(dbSession);
    List<String> uuids = items.stream()
      .map(EsQueueDto::getUuid)
      .filter(Objects::nonNull)
      .collect(toArrayList(items.size()));
    executeLargeUpdates(uuids, mapper::delete);
  }

  public Collection<EsQueueDto> selectForRecovery(DbSession dbSession, long beforeDate, long limit) {
    return mapper(dbSession).selectForRecovery(beforeDate, limit);
  }

  private static EsQueueMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(EsQueueMapper.class);
  }
}
