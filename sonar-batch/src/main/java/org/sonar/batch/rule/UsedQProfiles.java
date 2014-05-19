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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.batch.rule.ModuleQProfiles.QProfile;

import javax.annotation.concurrent.Immutable;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

@Immutable
public class UsedQProfiles {

  private Map<Integer, ModuleQProfiles.QProfile> profilesById = Maps.newLinkedHashMap();

  private UsedQProfiles() {
  }

  public static final UsedQProfiles fromProfiles(Iterable<QProfile> profiles) {
    UsedQProfiles result = new UsedQProfiles();
    for (QProfile qProfile : profiles) {
      result.add(qProfile);
    }
    return result;
  }

  public static final UsedQProfiles empty() {
    return new UsedQProfiles();
  }

  public static final UsedQProfiles fromProfiles(QProfile... profiles) {
    return fromProfiles(Arrays.asList(profiles));
  }

  public static final UsedQProfiles fromJSON(String json) {
    UsedQProfiles result = new UsedQProfiles();
    JsonArray root = new JsonParser().parse(json).getAsJsonArray();
    for (JsonElement elt : root) {
      JsonObject profile = elt.getAsJsonObject();
      result.add(new QProfile(profile.get("id").getAsInt(), profile.get("name").getAsString(), profile.get("language").getAsString(), profile.get("version").getAsInt()));
    }
    return result;
  }

  public final String toJSON() {
    StringWriter json = new StringWriter();
    JsonWriter writer = JsonWriter.of(json);
    writer.beginArray();
    for (ModuleQProfiles.QProfile qProfile : profilesById.values()) {
      writer.beginObject()
        .prop("id", qProfile.id())
        .prop("name", qProfile.name())
        .prop("version", qProfile.version())
        .prop("language", qProfile.language())
        .endObject();
    }
    writer.endArray();
    writer.close();
    return json.toString();
  }

  public final UsedQProfiles merge(UsedQProfiles other) {
    return empty().mergeInPlace(this).mergeInPlace(other);
  }

  private void add(ModuleQProfiles.QProfile profile) {
    QProfile alreadyAdded = profilesById.get(profile.id());
    if (alreadyAdded == null
      // Keep only latest version
      || profile.version() > alreadyAdded.version()) {
      profilesById.put(profile.id(), profile);
    }
  }

  private UsedQProfiles addAll(Iterable<QProfile> profiles) {
    for (QProfile profile : profiles) {
      this.add(profile);
    }
    return this;
  }

  private UsedQProfiles mergeInPlace(UsedQProfiles other) {
    this.addAll(other.profilesById.values());
    return this;
  }

  public Map<Integer, ModuleQProfiles.QProfile> profilesById() {
    return ImmutableMap.copyOf(profilesById);
  }

}
