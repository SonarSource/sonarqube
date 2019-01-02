/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.UtcDateUtils;

import static java.util.function.Function.identity;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

/**
 * Represents the array of JSON objects stored in the value of the
 * {@link org.sonar.api.measures.CoreMetrics#QUALITY_PROFILES} measures.
 */
@Immutable
public class QPMeasureData {

  private final SortedSet<QualityProfile> profiles;

  public QPMeasureData(Iterable<QualityProfile> qualityProfiles) {
    this.profiles = ImmutableSortedSet.copyOf(QualityProfileComparator.INSTANCE, qualityProfiles);
  }

  public static QPMeasureData fromJson(String json) {
    return new QPMeasureData(StreamSupport.stream(new JsonParser().parse(json).getAsJsonArray().spliterator(), false)
      .map(jsonElement -> {
        JsonObject jsonProfile = jsonElement.getAsJsonObject();
        return new QualityProfile(
          jsonProfile.get("key").getAsString(),
          jsonProfile.get("name").getAsString(),
          jsonProfile.get("language").getAsString(),
          UtcDateUtils.parseDateTime(jsonProfile.get("rulesUpdatedAt").getAsString()));
      }).collect(Collectors.toList()));
  }

  public static String toJson(QPMeasureData data) {
    StringWriter json = new StringWriter();
    try (JsonWriter writer = JsonWriter.of(json)) {
      writer.beginArray();
      for (QualityProfile profile : data.getProfiles()) {
        writer
          .beginObject()
          .prop("key", profile.getQpKey())
          .prop("language", profile.getLanguageKey())
          .prop("name", profile.getQpName())
          .prop("rulesUpdatedAt", UtcDateUtils.formatDateTime(profile.getRulesUpdatedAt()))
          .endObject();
      }
      writer.endArray();
    }
    return json.toString();
  }

  public SortedSet<QualityProfile> getProfiles() {
    return profiles;
  }

  public Map<String, QualityProfile> getProfilesByKey() {
    return profiles.stream().collect(uniqueIndex(QualityProfile::getQpKey, identity()));
  }

  private enum QualityProfileComparator implements Comparator<QualityProfile> {
    INSTANCE;

    @Override
    public int compare(QualityProfile o1, QualityProfile o2) {
      int c = o1.getLanguageKey().compareTo(o2.getLanguageKey());
      if (c == 0) {
        c = o1.getQpName().compareTo(o2.getQpName());
      }
      return c;
    }
  }
}
