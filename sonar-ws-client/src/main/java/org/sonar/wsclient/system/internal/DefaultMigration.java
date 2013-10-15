/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.wsclient.system.internal;

import org.sonar.wsclient.system.Migration;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

public class DefaultMigration implements Migration {

  private final Map json;

  public DefaultMigration(Map json) {
    this.json = json;
  }

  @Override
  public boolean operationalWebapp() {
    return JsonUtils.getBoolean(json, "operational");
  }

  @Override
  public Status status() {
    return Status.valueOf(JsonUtils.getString(json, "state"));
  }

  @Override
  public String message() {
    return JsonUtils.getString(json, "message");
  }

  @Override
  @Nullable
  public Date startedAt() {
    return JsonUtils.getDateTime(json, "startedAt");
  }
}
