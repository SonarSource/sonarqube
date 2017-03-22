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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileLookup;

public class InheritanceAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileLookup profileLookup;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;

  public InheritanceAction(DbClient dbClient, QProfileLookup profileLookup, QProfileWsSupport wsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.profileLookup = profileLookup;
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
      Statistics statistics = new Statistics(dbSession, organization);

      writeResponse(response.newJsonWriter(), profile, ancestors, children, statistics);
    }
  }

  private static void writeResponse(JsonWriter json, QualityProfileDto profile, List<QualityProfileDto> ancestors, List<QualityProfileDto> children, Statistics statistics) {
    json.beginObject();
    writeProfile(json, profile, statistics);
    writeAncestors(json, ancestors, statistics);
    writeChildren(json, children, statistics);
    json.endObject().close();
  }

  private static void writeProfile(JsonWriter json, QualityProfileDto profile, Statistics statistics) {
    String profileKey = profile.getKey();
    json.name("profile");
    writeProfileAttributes(json, profileKey, profile.getName(), profile.getParentKee(), statistics);
  }

  private static void writeAncestors(JsonWriter json, List<QualityProfileDto> ancestors, Statistics statistics) {
    json.name("ancestors").beginArray();
    for (QualityProfileDto ancestor : ancestors) {
      String ancestorKey = ancestor.getKey();
      writeProfileAttributes(json, ancestorKey, ancestor.getName(), ancestor.getParentKee(), statistics);
    }
    json.endArray();
  }

  private static void writeChildren(JsonWriter json, List<QualityProfileDto> children, Statistics statistics) {
    json.name("children").beginArray();
    for (QualityProfileDto child : children) {
      String childKey = child.getKey();
      writeProfileAttributes(json, childKey, child.getName(), null, statistics);
    }
    json.endArray();
  }

  private static void writeProfileAttributes(JsonWriter json, String key, String name, @Nullable String parentKey, Statistics statistics) {
    json.beginObject();
    json.prop("key", key)
      .prop("name", name)
      .prop("parent", parentKey);
    writeStats(json, key, statistics);
    json.endObject();
  }

  private static void writeStats(JsonWriter json, String profileKey, Statistics statistics) {
    json.prop("activeRuleCount", statistics.countRulesByProfileKey.getOrDefault(profileKey, 0L));
    json.prop("overridingRuleCount", statistics.countOverridingRulesByProfileKey.getOrDefault(profileKey, 0L));
  }

  private class Statistics {
    private final Map<String, Long> countRulesByProfileKey;
    private final Map<String, Long> countOverridingRulesByProfileKey;

    private Statistics(DbSession dbSession, OrganizationDto organization) {
      ActiveRuleDao dao = dbClient.activeRuleDao();
      countRulesByProfileKey = dao.countActiveRulesByProfileKey(dbSession, organization);
      countOverridingRulesByProfileKey = dao.countActiveRulesForInheritanceByProfileKey(dbSession, organization, ActiveRuleDto.OVERRIDES);
    }
  }
}
