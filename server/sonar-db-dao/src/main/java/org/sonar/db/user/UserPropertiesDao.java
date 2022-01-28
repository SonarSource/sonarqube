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
package org.sonar.db.user;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;

public class UserPropertiesDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public UserPropertiesDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  public List<UserPropertyDto> selectByUser(DbSession session, UserDto user) {
    return mapper(session).selectByUserUuid(user.getUuid());
  }

  public UserPropertyDto insertOrUpdate(DbSession session, UserPropertyDto dto, @Nullable String login) {
    long now = system2.now();
    boolean isUpdate = true;
    if (mapper(session).update(dto, now) == 0) {
      mapper(session).insert(dto.setUuid(uuidFactory.create()), now);
      isUpdate = false;
    }

    if (isUpdate) {
      auditPersister.updateProperty(session, new PropertyNewValue(dto, login), true);
    } else {
      auditPersister.addProperty(session, new PropertyNewValue(dto, login), true);
    }

    return dto;
  }

  public void deleteByUser(DbSession session, UserDto user) {
    List<UserPropertyDto> userProperties = selectByUser(session, user);
    int deletedRows = mapper(session).deleteByUserUuid(user.getUuid());

    if (deletedRows > 0) {
      userProperties.stream()
        .filter(p -> auditPersister.isTrackedProperty(p.getKey()))
        .forEach(p -> auditPersister.deleteProperty(session, new PropertyNewValue(p, user.getLogin()), true));
    }
  }

  private static UserPropertiesMapper mapper(DbSession session) {
    return session.getMapper(UserPropertiesMapper.class);
  }

}
