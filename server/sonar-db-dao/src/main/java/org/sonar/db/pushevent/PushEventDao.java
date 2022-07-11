/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.pushevent;

import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class PushEventDao implements Dao {

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public PushEventDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public PushEventDto insert(DbSession dbSession, PushEventDto event) {
    if (event.getUuid() == null) {
      event.setUuid(uuidFactory.create());
    }

    event.setCreatedAt(system2.now());
    mapper(dbSession).insert(event);
    return event;
  }

  public PushEventDto selectByUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectByUuid(uuid);
  }

  private static PushEventMapper mapper(DbSession session) {
    return session.getMapper(PushEventMapper.class);
  }

}
