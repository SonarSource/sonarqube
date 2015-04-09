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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.Multimap;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.search.FacetValue;

import java.util.List;
import java.util.Map;

public class QProfileInheritanceAction implements BaseQProfileWsAction {

  private final DbClient dbClient;

  private final QProfileLookup profileLookup;

  private final QProfileLoader profileLoader;

  private final QProfileFactory profileFactory;

  private final Languages languages;

  public QProfileInheritanceAction(DbClient dbClient, QProfileLookup profileLookup, QProfileLoader profileLoader, QProfileFactory profileFactory, Languages languages) {
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
      QualityProfileDto profile = dbClient.qualityProfileDao().getByKey(session, profileKey);
      if (profile == null) {
        throw new NotFoundException(String.format("Could not find a quality profile with key %s", profileKey));
      }

      List<QProfile> ancestors = profileLookup.ancestors(profile, session);
      List<QualityProfileDto> children = dbClient.qualityProfileDao().findChildren(session, profileKey);
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
    json.name("profile").beginObject()
      .prop("key", profileKey)
      .prop("name", profile.getName())
      .prop("parent", profile.getParentKee());
    writeStats(json, profileKey, profileStats);
    json.endObject();
  }

  private void writeAncestors(JsonWriter json, List<QProfile> ancestors, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.name("ancestors").beginArray();
    for (QProfile ancestor : ancestors) {
      String ancestorKey = ancestor.key();
      json.beginObject()
        .prop("key", ancestorKey)
        .prop("name", ancestor.name())
        .prop("parent", ancestor.parent());
      writeStats(json, ancestorKey, profileStats);
      json.endObject();
    }
    json.endArray();
  }

  private void writeChildren(JsonWriter json, List<QualityProfileDto> children, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.name("children").beginArray();
    for (QualityProfileDto child : children) {
      String childKey = child.getKey();
      json.beginObject()
        .prop("key", childKey)
        .prop("name", child.getName());
      writeStats(json, childKey, profileStats);
      json.endObject();
    }
    json.endArray();
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
    if (profileStats.containsKey("countActiveRules")) {
      result = profileStats.get("countActiveRules").iterator().next().getValue();
    }
    return result;
  }

  private Long getOverridingRuleCount(Multimap<String, FacetValue> profileStats) {
    Long result = null;
    if (profileStats.containsKey("inheritance")) {
      for (FacetValue value : profileStats.get("inheritance")) {
        if ("OVERRIDES".equals(value.getKey())) {
          result = value.getValue();
        }
      }
    }
    return result;
  }

}
