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
import java.util.stream.Stream;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class DeleteAction implements QProfileWsAction {

  private final Languages languages;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final QProfileWsSupport qProfileWsSupport;

  public DeleteAction(Languages languages, QProfileFactory profileFactory, DbClient dbClient, UserSession userSession, QProfileWsSupport qProfileWsSupport) {
    this.languages = languages;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("delete")
      .setDescription("Delete a quality profile and all its descendants. The default quality profile cannot be deleted. " +
        "Require Administer Quality Profiles permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    QProfileReference.defineParams(action, languages);
    QProfileWsSupport.createOrganizationParam(action).setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityProfileDto profile = qProfileWsSupport.getProfile(dbSession, QProfileReference.from(request));
      userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, profile.getOrganizationUuid());

      List<QualityProfileDto> descendants = selectDescendants(dbSession, profile);
      ensureNoneIsMarkedAsDefault(profile, descendants);

      profileFactory.deleteByKeys(dbSession, toKeys(profile, descendants));
      dbSession.commit();
    }
    response.noContent();
  }

  private List<QualityProfileDto> selectDescendants(DbSession dbSession, QualityProfileDto profile) {
    return dbClient.qualityProfileDao().selectDescendants(dbSession, profile.getKey());
  }

  private static void ensureNoneIsMarkedAsDefault(QualityProfileDto profile, List<QualityProfileDto> descendants) {
    checkArgument(!profile.isDefault(), "Profile '%s' cannot be deleted because it is marked as default", profile.getName());
    descendants.stream()
      .filter(QualityProfileDto::isDefault)
      .findFirst()
      .ifPresent(p -> {
        throw new IllegalArgumentException(String.format("Profile '%s' cannot be deleted because its descendant named '%s' is marked as default", profile.getName(), p.getName()));
      });
  }

  private static List<String> toKeys(QualityProfileDto profile, List<QualityProfileDto> descendants) {
    return Stream.concat(Stream.of(profile), descendants.stream())
      .map(QualityProfileDto::getKee)
      .collect(MoreCollectors.toList(descendants.size() + 1));
  }
}
