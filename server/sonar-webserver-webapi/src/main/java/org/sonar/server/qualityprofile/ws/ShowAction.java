/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleCountQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Qualityprofiles.ShowResponse;
import org.sonarqube.ws.Qualityprofiles.ShowResponse.CompareToSonarWay;
import org.sonarqube.ws.Qualityprofiles.ShowResponse.QualityProfile;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_COMPARE_TO_SONAR_WAY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;

public class ShowAction implements QProfileWsAction {

  private static final String SONAR_WAY = "Sonar way";
  private static final String SONARQUBE_WAY = "SonarQube way";

  private final DbClient dbClient;
  private final QProfileWsSupport qProfileWsSupport;
  private final Languages languages;

  public ShowAction(DbClient dbClient, QProfileWsSupport qProfileWsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.qProfileWsSupport = qProfileWsSupport;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction show = controller.createAction(ACTION_SHOW)
      .setDescription("Show a quality profile")
      .setSince("6.5")
      .setResponseExample(getClass().getResource("show-example.json"))
      .setInternal(true)
      .setHandler(this);

    show.createParam(PARAM_KEY)
      .setDescription("Quality profile key")
      .setExampleValue(UUID_EXAMPLE_01)
      .setRequired(true);

    show.createParam(PARAM_COMPARE_TO_SONAR_WAY)
      .setDescription("Add the number of missing rules from the related Sonar way profile in the response")
      .setInternal(true)
      .setDefaultValue("false")
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = qProfileWsSupport.getProfile(dbSession, QProfileReference.fromKey(request.mandatoryParam(PARAM_KEY)));
      boolean isDefault = dbClient.defaultQProfileDao().isDefault(dbSession, profile.getKee());
      ActiveRuleCountQuery.Builder builder = ActiveRuleCountQuery.builder();
      long activeRuleCount = countActiveRulesByQuery(dbSession, profile, builder);
      long deprecatedActiveRuleCount = countActiveRulesByQuery(dbSession, profile, builder.setRuleStatus(DEPRECATED));
      long projectCount = countProjectsByProfiles(dbSession, profile);
      CompareToSonarWay compareToSonarWay = getSonarWay(request, dbSession, profile);
      writeProtobuf(buildResponse(profile, isDefault, getLanguage(profile), activeRuleCount, deprecatedActiveRuleCount, projectCount, compareToSonarWay), request, response);
    }
  }

  private long countActiveRulesByQuery(DbSession dbSession, QProfileDto profile, ActiveRuleCountQuery.Builder queryBuilder) {
    Map<String, Long> result = dbClient.activeRuleDao().countActiveRulesByQuery(dbSession, queryBuilder.setProfiles(singletonList(profile)).build());
    return result.getOrDefault(profile.getKee(), 0L);
  }

  private long countProjectsByProfiles(DbSession dbSession, QProfileDto profile) {
    Map<String, Long> projects = dbClient.qualityProfileDao().countProjectsByProfiles(dbSession, singletonList(profile));
    return projects.getOrDefault(profile.getKee(), 0L);
  }

  public Language getLanguage(QProfileDto profile) {
    Language language = languages.get(profile.getLanguage());
    checkFound(language, "Quality Profile with key '%s' does not exist", profile.getKee());
    return language;
  }

  @CheckForNull
  public CompareToSonarWay getSonarWay(Request request, DbSession dbSession, QProfileDto profile) {
    if (!request.mandatoryParamAsBoolean(PARAM_COMPARE_TO_SONAR_WAY) || profile.isBuiltIn()) {
      return null;
    }
    QProfileDto sonarWay = Stream.of(SONAR_WAY, SONARQUBE_WAY)
      .map(name -> dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, name, profile.getLanguage()))
      .filter(Objects::nonNull)
      .filter(QProfileDto::isBuiltIn)
      .findFirst()
      .orElse(null);

    if (sonarWay == null) {
      return null;
    }

    long missingRuleCount = dbClient.activeRuleDao().countMissingRules(dbSession, profile.getRulesProfileUuid(), sonarWay.getRulesProfileUuid());

    return CompareToSonarWay.newBuilder()
      .setProfile(sonarWay.getKee())
      .setProfileName(sonarWay.getName())
      .setMissingRuleCount(missingRuleCount)
      .build();
  }

  private static ShowResponse buildResponse(QProfileDto profile, boolean isDefault, Language language, long activeRules, long deprecatedActiveRules, long projects,
                                            @Nullable CompareToSonarWay compareToSonarWay) {
    ShowResponse.Builder showResponseBuilder = Qualityprofiles.ShowResponse.newBuilder();
    QualityProfile.Builder profileBuilder = QualityProfile.newBuilder()
      .setKey(profile.getKee())
      .setName(profile.getName())
      .setLanguage(profile.getLanguage())
      .setLanguageName(language.getName())
      .setIsBuiltIn(profile.isBuiltIn())
      .setIsDefault(isDefault)
      .setIsInherited(profile.getParentKee() != null)
      .setActiveRuleCount(activeRules)
      .setActiveDeprecatedRuleCount(deprecatedActiveRules)
      .setProjectCount(projects);
    ofNullable(profile.getRulesUpdatedAt()).ifPresent(profileBuilder::setRulesUpdatedAt);
    ofNullable(profile.getLastUsed()).ifPresent(last -> profileBuilder.setLastUsed(formatDateTime(last)));
    ofNullable(profile.getUserUpdatedAt()).ifPresent(userUpdatedAt -> profileBuilder.setUserUpdatedAt(formatDateTime(userUpdatedAt)));
    ofNullable(compareToSonarWay).ifPresent(showResponseBuilder::setCompareToSonarWay);
    return showResponseBuilder.setProfile(profileBuilder).build();
  }

}
