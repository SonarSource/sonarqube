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
package org.sonar.server.user.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.user.User;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserDoc;

import static org.sonar.server.ws.JsonWriterUtils.isFieldNeeded;
import static org.sonar.server.ws.JsonWriterUtils.writeIfNeeded;

public class UserJsonWriter {

  private static final String FIELD_LOGIN = "login";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_SCM_ACCOUNTS = "scmAccounts";
  private static final String FIELD_GROUPS = "groups";
  private static final String FIELD_ACTIVE = "active";

  public static final Set<String> FIELDS = ImmutableSet.of(FIELD_NAME, FIELD_EMAIL, FIELD_SCM_ACCOUNTS, FIELD_GROUPS, FIELD_ACTIVE);
  private static final Set<String> CONCISE_FIELDS = ImmutableSet.of(FIELD_NAME, FIELD_EMAIL, FIELD_ACTIVE);

  private final UserSession userSession;

  public UserJsonWriter(UserSession userSession) {
    this.userSession = userSession;
  }

  /**
   * Serializes a user to the passed JsonWriter.
   */
  public void write(JsonWriter json, User user, Collection<String> groups, Collection<String> fields) {
    json.beginObject();
    json.prop(FIELD_LOGIN, user.login());
    writeIfNeeded(json, user.name(), FIELD_NAME, fields);
    writeIfNeeded(json, user.email(), FIELD_EMAIL, fields);
    writeIfNeeded(json, user.active(), FIELD_ACTIVE, fields);
    writeGroupsIfNeeded(json, groups, fields);
    writeScmAccountsIfNeeded(json, fields, user);
    json.endObject();
  }

  /**
   * A shortcut to {@link #write(JsonWriter, User, Collection, Collection)} with preselected fields and without group information
   */
  public void write(JsonWriter json, @Nullable User user) {
    if (user == null) {
      json.beginObject().endObject();
    } else {
      write(json, user, ImmutableSet.<String>of(), CONCISE_FIELDS);
    }
  }

  private void writeGroupsIfNeeded(JsonWriter json, Collection<String> groups, @Nullable Collection<String> fields) {
    if (isFieldNeeded(FIELD_GROUPS, fields) && userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)) {
      json.name(FIELD_GROUPS).beginArray();
      for (String groupName : groups) {
        json.value(groupName);
      }
      json.endArray();
    }
  }

  private static void writeScmAccountsIfNeeded(JsonWriter json, Collection<String> fields, User user) {
    if (isFieldNeeded(FIELD_SCM_ACCOUNTS, fields)) {
      json.name(FIELD_SCM_ACCOUNTS)
        .beginArray()
        .values(((UserDoc) user).scmAccounts())
        .endArray();
    }
  }
}
