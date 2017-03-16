/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.search.FacetValue;

import static org.sonar.server.qualityprofile.index.ActiveRuleIndex.COUNT_ACTIVE_RULES;

public class InheritanceAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileLookup profileLookup;
  private final ActiveRuleIndex activeRuleIndex;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;

  public InheritanceAction(DbClient dbClient, QProfileLookup profileLookup, ActiveRuleIndex activeRuleIndex, QProfileWsSupport wsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.profileLookup = profileLookup;
    this.activeRuleIndex = activeRuleIndex;
    this.wsSupport = wsSupport;
    this.languages = languages;
  }

  @Override
  public void define(NewController context) {
    NewAction inheritance = context.createAction("inheritance")
      .setSince("5.2")
      .setDescription("Show a quality profile's ancestors and children.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-inheritance.json"));

    QProfileWsSupport.createOrganizationParam(inheritance)
      .setSince("6.4");
    QProfileReference.defineParams(inheritance, languages);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    QProfileReference reference = QProfileReference.from(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityProfileDto profile = wsSupport.getProfile(dbSession, reference);
      String organizationUuid = profile.getOrganizationUuid();
      OrganizationDto organization = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
        .orElseThrow(() -> new IllegalStateException(String.format("Could not find organization with uuid '%s' for quality profile '%s'", organizationUuid, profile.getKee())));
      List<QualityProfileDto> ancestors = profileLookup.ancestors(profile, dbSession);
      List<QualityProfileDto> children = dbClient.qualityProfileDao().selectChildren(dbSession, profile.getKey());
      Map<String, Multimap<String, FacetValue>> profileStats = getAllProfileStats(dbSession, organization);

      writeResponse(response.newJsonWriter(), profile, ancestors, children, profileStats);
    }
  }

  @VisibleForTesting
  Map<String, Multimap<String, FacetValue>> getAllProfileStats(DbSession dbSession, OrganizationDto organization) {
    List<String> keys = dbClient.qualityProfileDao().selectAll(dbSession, organization).stream().map(QualityProfileDto::getKey).collect(Collectors.toList());
    return activeRuleIndex.getStatsByProfileKeys(keys);
  }

  private static void writeResponse(JsonWriter json, QualityProfileDto profile, List<QualityProfileDto> ancestors, List<QualityProfileDto> children,
    Map<String, Multimap<String, FacetValue>> profileStats) {
    json.beginObject();
    writeProfile(json, profile, profileStats);
    writeAncestors(json, ancestors, profileStats);
    writeChildren(json, children, profileStats);
    json.endObject().close();
  }

  private static void writeProfile(JsonWriter json, QualityProfileDto profile, Map<String, Multimap<String, FacetValue>> profileStats) {
    String profileKey = profile.getKey();
    json.name("profile");
    writeProfileAttributes(json, profileKey, profile.getName(), profile.getParentKee(), profileStats);
  }

  private static void writeAncestors(JsonWriter json, List<QualityProfileDto> ancestors, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.name("ancestors").beginArray();
    for (QualityProfileDto ancestor : ancestors) {
      String ancestorKey = ancestor.getKey();
      writeProfileAttributes(json, ancestorKey, ancestor.getName(), ancestor.getParentKee(), profileStats);
    }
    json.endArray();
  }

  private static void writeChildren(JsonWriter json, List<QualityProfileDto> children, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.name("children").beginArray();
    for (QualityProfileDto child : children) {
      String childKey = child.getKey();
      writeProfileAttributes(json, childKey, child.getName(), null, profileStats);
    }
    json.endArray();
  }

  private static void writeProfileAttributes(JsonWriter json, String key, String name, @Nullable String parentKey, Map<String, Multimap<String, FacetValue>> profileStats) {
    json.beginObject();
    json.prop("key", key)
      .prop("name", name)
      .prop("parent", parentKey);
    writeStats(json, key, profileStats);
    json.endObject();
  }

  private static void writeStats(JsonWriter json, String profileKey, Map<String, Multimap<String, FacetValue>> profileStats) {
    if (profileStats.containsKey(profileKey)) {
      Multimap<String, FacetValue> ancestorStats = profileStats.get(profileKey);
      json.prop("activeRuleCount", getActiveRuleCount(ancestorStats));
      json.prop("overridingRuleCount", getOverridingRuleCount(ancestorStats));
    } else {
      json.prop("activeRuleCount", 0);
    }
  }

  private static Long getActiveRuleCount(Multimap<String, FacetValue> profileStats) {
    Long result = null;
    if (profileStats.containsKey(COUNT_ACTIVE_RULES)) {
      result = profileStats.get(COUNT_ACTIVE_RULES).iterator().next().getValue();
    }
    return result;
  }

  private static Long getOverridingRuleCount(Multimap<String, FacetValue> profileStats) {
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
