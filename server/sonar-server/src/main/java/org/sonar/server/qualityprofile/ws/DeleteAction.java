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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singleton;
import static org.sonar.server.qualityprofile.ws.QProfileWsSupport.createOrganizationParam;

public class DeleteAction implements QProfileWsAction {

  private final Languages languages;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;

  public DeleteAction(Languages languages, QProfileFactory profileFactory, DbClient dbClient, UserSession userSession, QProfileWsSupport wsSupport) {
    this.languages = languages;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("delete")
      .setDescription("Delete a quality profile and all its descendants. The default quality profile cannot be deleted.<br> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    QProfileReference.defineParams(action, languages);
    createOrganizationParam(action)
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.from(request));
      OrganizationDto organization = wsSupport.getOrganization(dbSession, profile);
      wsSupport.checkCanEdit(dbSession, organization, profile);

      Collection<QProfileDto> descendants = selectDescendants(dbSession, profile);
      ensureNoneIsMarkedAsDefault(dbSession, profile, descendants);

      profileFactory.delete(dbSession, merge(profile, descendants));
      dbSession.commit();
    }
    response.noContent();
  }

  private Collection<QProfileDto> selectDescendants(DbSession dbSession, QProfileDto profile) {
    return dbClient.qualityProfileDao().selectDescendants(dbSession, singleton(profile));
  }

  private void ensureNoneIsMarkedAsDefault(DbSession dbSession, QProfileDto profile, Collection<QProfileDto> descendants) {
    Set<String> allUuids = new HashSet<>();
    allUuids.add(profile.getKee());
    descendants.forEach(p -> allUuids.add(p.getKee()));

    Set<String> uuidsOfDefaultProfiles = dbClient.defaultQProfileDao().selectExistingQProfileUuids(dbSession, profile.getOrganizationUuid(), allUuids);

    checkArgument(!uuidsOfDefaultProfiles.contains(profile.getKee()), "Profile '%s' cannot be deleted because it is marked as default", profile.getName());
    descendants.stream()
      .filter(p -> uuidsOfDefaultProfiles.contains(p.getKee()))
      .findFirst()
      .ifPresent(p -> {
        throw new IllegalArgumentException(String.format("Profile '%s' cannot be deleted because its descendant named '%s' is marked as default", profile.getName(), p.getName()));
      });
  }

  private static List<QProfileDto> merge(QProfileDto profile, Collection<QProfileDto> descendants) {
    return Stream.concat(Stream.of(profile), descendants.stream())
      .collect(MoreCollectors.toList(descendants.size() + 1));
  }
}
