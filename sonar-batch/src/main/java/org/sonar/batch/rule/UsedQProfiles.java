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
package org.sonar.batch.rule;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.UtcDateUtils;

import javax.annotation.concurrent.Immutable;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

@Immutable
public class UsedQProfiles {

  private final SortedSet<QProfile> profiles = Sets.newTreeSet(new Comparator<QProfile>() {
    @Override
    public int compare(QProfile o1, QProfile o2) {
      int c = o1.getLanguage().compareTo(o2.getLanguage());
      if (c == 0) {
        c = o1.getName().compareTo(o2.getName());
      }
      return c;
    }
  });

  public static UsedQProfiles fromJson(String json) {
    UsedQProfiles result = new UsedQProfiles();
    JsonArray jsonRoot = new JsonParser().parse(json).getAsJsonArray();
    for (JsonElement jsonElt : jsonRoot) {
      JsonObject jsonProfile = jsonElt.getAsJsonObject();
      QProfile profile = new QProfile();
      profile.setKey(jsonProfile.get("key").getAsString());
      profile.setName(jsonProfile.get("name").getAsString());
      profile.setLanguage(jsonProfile.get("language").getAsString());
      profile.setRulesUpdatedAt(UtcDateUtils.parseDateTime(jsonProfile.get("rulesUpdatedAt").getAsString()));
      result.add(profile);
    }
    return result;
  }

  public String toJson() {
    StringWriter json = new StringWriter();
    JsonWriter writer = JsonWriter.of(json);
    writer.beginArray();
    for (QProfile profile : profiles) {
      writer
        .beginObject()
        .prop("key", profile.getKey())
        .prop("language", profile.getLanguage())
        .prop("name", profile.getName())
        .prop("rulesUpdatedAt", UtcDateUtils.formatDateTime(profile.getRulesUpdatedAt()))
        .endObject();
    }
    writer.endArray();
    writer.close();
    return json.toString();
  }

  public UsedQProfiles add(UsedQProfiles other) {
    addAll(other.profiles);
    return this;
  }

  public UsedQProfiles add(QProfile profile) {
    profiles.add(profile);
    return this;
  }

  public UsedQProfiles addAll(Collection<QProfile> profiles) {
    this.profiles.addAll(profiles);
    return this;
  }

  public SortedSet<QProfile> profiles() {
    return profiles;
  }

  public Map<String, QProfile> profilesByKey() {
    Map<String, QProfile> map = new HashMap<>();
    for (QProfile profile : profiles) {
      map.put(profile.getKey(), profile);
    }
    return map;
  }
}
