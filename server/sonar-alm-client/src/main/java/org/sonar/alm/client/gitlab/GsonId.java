/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.alm.client.gitlab;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.util.Collection;

public class GsonId {
  private static final TypeToken<Collection<GsonId>> COLLECTION_TYPE_TOKEN = new TypeToken<>() {
  };

  @SerializedName("id")
  private final long id;

  public GsonId() {
    // http://stackoverflow.com/a/18645370/229031
    this(0);
  }

  public GsonId(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public static GsonId parseOne(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, GsonId.class);
  }

  public static Collection<GsonId> parseCollection(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, COLLECTION_TYPE_TOKEN);
  }

}
