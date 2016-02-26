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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.search.FacetValue;

import static org.sonar.server.qualityprofile.index.ActiveRuleIndex.COUNT_ACTIVE_RULES;

public class InheritanceAction implements QProfileWsAction {

  private final DbClient dbClient;

  private final QProfileLookup profileLookup;

  private final QProfileLoader profileLoader;

  private final QProfileFactory profileFactory;

  private final Languages languages;

  public InheritanceAction(DbClient dbClient, QProfileLookup profileLookup, QProfileLoader profileLoader, QProfileFactory profileFactory, Languages languages) {
    this.dbClient = dbClient;
    this.profileLookup = profileLookup;
    this.profileLoader = profileLoader;
    this.profileFactory = profileFactory;
    this.languages = languages;
  }

  @Override
  public void define(NewController context) {
    NewAction inheritance = context.createAction("inheritance")
      .setSince("5.2")
      .setDescription("Show a quality profile's ancestors and children.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-inheritance.json"));

    QProfileIdentificationParamUtils.defineProfileParams(inheritance, languages);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession session = dbClient.openSession(false);
    try {
      String profileKey = QProfileIdentificationParamUtils.getProfileKeyFromParameters(request, profileFactory, session);
      QualityProfileDto profile = dbClient.qualityProfileDao().selectByKey(session, profileKey);
      if (profile == null) {
        throw new NotFoundException(String.format("Could not find a quality profile with key %s", profileKey));
      }

      List<QProfile> ancestors = profileLookup.ancestors(profile, session);
      List<QualityProfileDto> children = dbClient.qualityProfileDao().selectChildren(session, profileKey);
      Map<String, Multimap<String, FacetValue>> profileStats = profileLoader.getAllProfileStats();

      writeResponse(response.newJsonWriter(), profile, ancestors, children, profileStats);
    } finally {
      session.close();
    }
  }

  private void writeResponse(JsonWriter json, QualityProfileDto profile, List<QProfile> ancestors, List<QualityProfileDto> children,
    Map<String, Multimap<String, FacetValue>> profileStats) {
    json.beginObject();
    writeProfile(json, profile, profileStats);
    writeAncestors(json, ancestors, profileStats);
    writeChildren(json, children, profileStats);
    json.endObject().close();
  }

  private void writeProfile(JsonWriter json, QualityProfileDto profile, Map<String, Multimap<String, FacetValue>> profileStats) {
    String profileKey = profile.getKey();
    json.name("profile");
    writeProfileAttributes(json, profileKey, profile.getName(), profile.getParentKee(), profileStats);
  }

  private void writeAncestors(JsonWriter json, List<QProfile> ancestors, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.name("ancestors").beginArray();
    for (QProfile ancestor : ancestors) {
      String ancestorKey = ancestor.key();
      writeProfileAttributes(json, ancestorKey, ancestor.name(), ancestor.parent(), profileStats);
    }
    json.endArray();
  }

  private void writeChildren(JsonWriter json, List<QualityProfileDto> children, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.name("children").beginArray();
    for (QualityProfileDto child : children) {
      String childKey = child.getKey();
      writeProfileAttributes(json, childKey, child.getName(), null, profileStats);
    }
    json.endArray();
  }

  private void writeProfileAttributes(JsonWriter json, String key, String name, @Nullable String parentKey, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.beginObject();
    json.prop("key", key)
      .prop("name", name)
      .prop("parent", parentKey);
    writeStats(json, key, profileStats);
    json.endObject();
  }

  private void writeStats(JsonWriter json, String profileKey, Map<String, Multimap<String, FacetValue>> profileStats) {
    if (profileStats.containsKey(profileKey)) {
      Multimap<String, FacetValue> ancestorStats = profileStats.get(profileKey);
      json.prop("activeRuleCount", getActiveRuleCount(ancestorStats));
      json.prop("overridingRuleCount", getOverridingRuleCount(ancestorStats));
    } else {
      json.prop("activeRuleCount", 0);
    }
  }

  private Long getActiveRuleCount(Multimap<String, FacetValue> profileStats) {
    Long result = null;
    if (profileStats.containsKey(COUNT_ACTIVE_RULES)) {
      result = profileStats.get(COUNT_ACTIVE_RULES).iterator().next().getValue();
    }
    return result;
  }

  private Long getOverridingRuleCount(Multimap<String, FacetValue> profileStats) {
    Long result = null;
    if (profileStats.containsKey(RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE)) {
      for (FacetValue value : profileStats.get(RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE)) {
        if ("OVERRIDES".equals(value.getKey())) {
          result = value.getValue();
        }
      }
    }
    return result;
  }

}
