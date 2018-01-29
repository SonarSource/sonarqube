/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleCountQuery;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonarqube.ws.Qualityprofiles.InheritanceWsResponse;
import org.sonarqube.ws.Qualityprofiles.InheritanceWsResponse.QualityProfile;

import static java.util.Collections.singleton;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.server.qualityprofile.ws.QProfileWsSupport.createOrganizationParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class InheritanceAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;

  public InheritanceAction(DbClient dbClient, QProfileWsSupport wsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.languages = languages;
  }

  @Override
  public void define(NewController context) {
    NewAction inheritance = context.createAction("inheritance")
      .setSince("5.2")
      .setDescription("Show a quality profile's ancestors and children.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("inheritance-example.json"));

    createOrganizationParam(inheritance)
      .setSince("6.4");
    QProfileReference.defineParams(inheritance, languages);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    QProfileReference reference = QProfileReference.from(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, reference);
      OrganizationDto organization = wsSupport.getOrganization(dbSession, profile);
      List<QProfileDto> ancestors = ancestors(profile, dbSession);
      List<QProfileDto> children = dbClient.qualityProfileDao().selectChildren(dbSession, singleton(profile));
      List<QProfileDto> allProfiles = new ArrayList<>();
      allProfiles.add(profile);
      allProfiles.addAll(ancestors);
      allProfiles.addAll(children);
      Statistics statistics = new Statistics(dbSession, organization, allProfiles);

      writeProtobuf(buildResponse(profile, ancestors, children, statistics), request, response);
    }
  }

  private List<QProfileDto> ancestors(QProfileDto profile, DbSession dbSession) {
    List<QProfileDto> ancestors = new ArrayList<>();
    collectAncestors(profile, ancestors, dbSession);
    return ancestors;
  }

  private void collectAncestors(QProfileDto profile, List<QProfileDto> ancestors, DbSession session) {
    if (profile.getParentKee() == null) {
      return;
    }

    QProfileDto parent = getParent(session, profile);
    ancestors.add(parent);
    collectAncestors(parent, ancestors, session);
  }

  private QProfileDto getParent(DbSession dbSession, QProfileDto profile) {
    QProfileDto parent = dbClient.qualityProfileDao().selectByUuid(dbSession, profile.getParentKee());
    if (parent == null) {
      throw new IllegalStateException("Cannot find parent of profile: " + profile.getKee());
    }
    return parent;
  }

  private static InheritanceWsResponse buildResponse(QProfileDto profile, List<QProfileDto> ancestors, List<QProfileDto> children, Statistics statistics) {
    return InheritanceWsResponse.newBuilder()
      .setProfile(buildProfile(profile, statistics))
      .addAllAncestors(buildAncestors(ancestors, statistics))
      .addAllChildren(buildChildren(children, statistics))
      .build();
  }

  private static Iterable<QualityProfile> buildAncestors(List<QProfileDto> ancestors, Statistics statistics) {
    return ancestors.stream()
      .map(ancestor -> buildProfile(ancestor, statistics))
      .collect(Collectors.toList());
  }

  private static Iterable<QualityProfile> buildChildren(List<QProfileDto> children, Statistics statistics) {
    return children.stream()
      .map(child -> buildProfile(child, statistics))
      .collect(Collectors.toList());
  }

  private static QualityProfile buildProfile(QProfileDto qualityProfile, Statistics statistics) {
    String key = qualityProfile.getKee();
    QualityProfile.Builder builder = QualityProfile.newBuilder()
      .setKey(key)
      .setName(qualityProfile.getName())
      .setActiveRuleCount(statistics.countRulesByProfileKey.getOrDefault(key, 0L))
      .setOverridingRuleCount(statistics.countOverridingRulesByProfileKey.getOrDefault(key, 0L))
      .setIsBuiltIn(qualityProfile.isBuiltIn());
    setNullable(qualityProfile.getParentKee(), builder::setParent);
    return builder.build();
  }

  private class Statistics {
    private final Map<String, Long> countRulesByProfileKey;
    private final Map<String, Long> countOverridingRulesByProfileKey;

    private Statistics(DbSession dbSession, OrganizationDto organization, List<QProfileDto> profiles) {
      ActiveRuleDao dao = dbClient.activeRuleDao();
      ActiveRuleCountQuery.Builder builder = ActiveRuleCountQuery.builder().setOrganization(organization);
      countRulesByProfileKey = dao.countActiveRulesByQuery(dbSession, builder.setProfiles(profiles).build());
      countOverridingRulesByProfileKey = dao.countActiveRulesByQuery(dbSession, builder.setProfiles(profiles).setInheritance(OVERRIDES).build());
    }
  }
}
