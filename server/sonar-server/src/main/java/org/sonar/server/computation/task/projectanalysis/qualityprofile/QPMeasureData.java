/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.qualityprofile;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.UtcDateUtils;

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
    return new QPMeasureData(Iterables.transform(new JsonParser().parse(json).getAsJsonArray(), JsonElementToQualityProfile.INSTANCE));
  }

  public static String toJson(QPMeasureData data) {
    StringWriter json = new StringWriter();
    JsonWriter writer = JsonWriter.of(json);
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
    writer.close();
    return json.toString();
  }

  public SortedSet<QualityProfile> getProfiles() {
    return profiles;
  }

  public Map<String, QualityProfile> getProfilesByKey() {
    return Maps.uniqueIndex(this.profiles, QualityProfileToKey.INSTANCE);
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

  private enum JsonElementToQualityProfile implements Function<JsonElement, QualityProfile> {
    INSTANCE;

    @Override
    public QualityProfile apply(@Nonnull JsonElement jsonElt) {
      JsonObject jsonProfile = jsonElt.getAsJsonObject();
      return new QualityProfile(
          jsonProfile.get("key").getAsString(),
          jsonProfile.get("name").getAsString(),
          jsonProfile.get("language").getAsString(),
          UtcDateUtils.parseDateTime(jsonProfile.get("rulesUpdatedAt").getAsString()));
    }
  }

  private enum QualityProfileToKey implements Function<QualityProfile, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull QualityProfile input) {
      return input.getQpKey();
    }
  }
}
