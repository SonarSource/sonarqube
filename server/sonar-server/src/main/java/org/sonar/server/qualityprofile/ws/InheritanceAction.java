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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonarqube.ws.QualityProfiles.InheritanceWsResponse;
import org.sonarqube.ws.QualityProfiles.InheritanceWsResponse.QualityProfile;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

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

      writeProtobuf(buildResponse(profile, ancestors, children, statistics), request, response);
    }
  }

  private static InheritanceWsResponse buildResponse(QualityProfileDto profile, List<QualityProfileDto> ancestors, List<QualityProfileDto> children, Statistics statistics) {
    return InheritanceWsResponse.newBuilder()
      .setProfile(buildProfile(profile, statistics))
      .addAllAncestors(buildAncestors(ancestors, statistics))
      .addAllChildren(buildChildren(children, statistics))
      .build();
  }

  private static QualityProfile buildProfile(QualityProfileDto profile, Statistics statistics) {
    return buildProfile(profile.getKey(), profile.getName(), profile.getParentKee(), statistics);
  }

  private static Iterable<QualityProfile> buildAncestors(List<QualityProfileDto> ancestors, Statistics statistics) {
    return ancestors.stream()
      .map(ancestor -> buildProfile(ancestor.getKey(), ancestor.getName(), ancestor.getParentKee(), statistics))
      .collect(Collectors.toList());
  }

  private static Iterable<QualityProfile> buildChildren(List<QualityProfileDto> children, Statistics statistics) {
    return children.stream()
      .map(child -> buildProfile(child.getKey(), child.getName(), null, statistics))
      .collect(Collectors.toList());
  }

  private static QualityProfile buildProfile(String key, String name, @Nullable String parentKey, Statistics statistics) {
    QualityProfile.Builder builder = QualityProfile.newBuilder()
      .setKey(key)
      .setName(name)
      .setActiveRuleCount(statistics.countRulesByProfileKey.getOrDefault(key, 0L))
      .setOverridingRuleCount(statistics.countOverridingRulesByProfileKey.getOrDefault(key, 0L));
    Protobuf.setNullable(parentKey, builder::setParent);
    return builder.build();
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
