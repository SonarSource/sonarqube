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
import org.sonar.server.qualityprofile.QProfileLookup;

import java.util.List;

public class QProfileInheritanceAction implements BaseQProfileWsAction {

  private final DbClient dbClient;

  private final QProfileLookup profileLookup;

  private final QProfileFactory profileFactory;

  private final Languages languages;

  public QProfileInheritanceAction(DbClient dbClient, QProfileLookup profileLookup, QProfileFactory profileFactory, Languages languages) {
    this.dbClient = dbClient;
    this.profileLookup = profileLookup;
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

      writeResponse(response.newJsonWriter(), ancestors, children);
    } finally {
      session.close();
    }
  }

  private void writeResponse(JsonWriter json, List<QProfile> ancestors, List<QualityProfileDto> children) {
    json.beginObject();
    writeAncestors(json, ancestors);
    writeChildren(json, children);
    json.endObject().close();
  }

  private void writeAncestors(JsonWriter json, List<QProfile> ancestors) {
    json.name("ancestors").beginArray();
    for (QProfile ancestor : ancestors) {
      json.beginObject()
        .prop("key", ancestor.key())
        .prop("name", ancestor.name())
        .prop("parent", ancestor.parent())
        .endObject();
    }
    json.endArray();
  }

  private void writeChildren(JsonWriter json, List<QualityProfileDto> children) {
    json.name("children").beginArray();
    for (QualityProfileDto child : children) {
      json.beginObject()
        .prop("key", child.getKey())
        .prop("name", child.getName())
        .endObject();
    }
    json.endArray();
  }
}
