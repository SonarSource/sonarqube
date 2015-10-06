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
package org.sonar.server.usergroups.ws;

import com.google.common.base.Preconditions;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;

@ServerSide
public class UserGroupUpdater {

  static final String PARAM_ID = "id";
  static final String PARAM_DESCRIPTION = "description";
  static final String PARAM_NAME = "name";

  // Database column size should be 500 (since migration #353),
  // but on some instances, column size is still 255,
  // hence the validation is done with 255
  static final int NAME_MAX_LENGTH = 255;
  static final int DESCRIPTION_MAX_LENGTH = 200;

  private final DbClient dbClient;

  public UserGroupUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  protected void validateName(String name) {
    checkNameLength(name);
    checkNameNotAnyone(name);
  }

  private void checkNameLength(String name) {
    Preconditions.checkArgument(!name.isEmpty(), "Name cannot be empty");
    Preconditions.checkArgument(name.length() <= NAME_MAX_LENGTH, String.format("Name cannot be longer than %d characters", NAME_MAX_LENGTH));
  }

  private void checkNameNotAnyone(String name) {
    Preconditions.checkArgument(!DefaultGroups.isAnyone(name), String.format("Name '%s' is reserved (regardless of case)", DefaultGroups.ANYONE));
  }

  protected void checkNameIsUnique(String name, DbSession session) {
    // There is no database constraint on column groups.name
    // because MySQL cannot create a unique index
    // on a UTF-8 VARCHAR larger than 255 characters on InnoDB
    if (dbClient.groupDao().selectByName(session, name) != null) {
      throw new BadRequestException(String.format("Name '%s' is already taken", name));
    }
  }

  protected void validateDescription(String description) {
    Preconditions.checkArgument(description.length() <= DESCRIPTION_MAX_LENGTH, String.format("Description cannot be longer than %d characters", DESCRIPTION_MAX_LENGTH));
  }

  protected void writeGroup(JsonWriter json, GroupDto group, int membersCount) {
    json.name("group").beginObject()
      .prop(PARAM_ID, group.getId().toString())
      .prop(PARAM_NAME, group.getName())
      .prop(PARAM_DESCRIPTION, group.getDescription())
      .prop("membersCount", membersCount)
      .endObject();
  }
}
