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

import java.util.Date;
import java.util.Map;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;

import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TO;

public class ChangelogAction implements QProfileWsAction {

  private final ChangelogLoader changelogLoader;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;
  private DbClient dbClient;

  public ChangelogAction(ChangelogLoader changelogLoader, QProfileWsSupport wsSupport, Languages languages, DbClient dbClient) {
    this.changelogLoader = changelogLoader;
    this.wsSupport = wsSupport;
    this.languages = languages;
    this.dbClient = dbClient;
  }

  @Override
  public void define(NewController context) {
    NewAction wsAction = context.createAction("changelog")
      .setSince("5.2")
      .setDescription("Get the history of changes on a quality profile: rule activation/deactivation, change in parameters/severity. " +
        "Events are ordered by date in descending order (most recent first).")
      .setHandler(this)
      .setResponseExample(getClass().getResource("changelog-example.json"));

    QProfileWsSupport.createOrganizationParam(wsAction)
      .setSince("6.4");

    QProfileReference.defineParams(wsAction, languages);

    wsAction.addPagingParams(50, MAX_LIMIT);

    wsAction.createParam(PARAM_SINCE)
      .setDescription("Start date for the changelog.")
      .setExampleValue("2011-04-25T01:15:42+0100");

    wsAction.createParam(PARAM_TO)
      .setDescription("End date for the changelog.")
      .setExampleValue("2013-07-25T07:35:42+0200");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    QProfileReference reference = QProfileReference.from(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, reference);

      QProfileChangeQuery query = new QProfileChangeQuery(profile.getKee());
      Date since = parseStartingDateOrDateTime(request.param(PARAM_SINCE));
      if (since != null) {
        query.setFromIncluded(since.getTime());
      }
      Date to = parseEndingDateOrDateTime(request.param(PARAM_TO));
      if (to != null) {
        query.setToExcluded(to.getTime());
      }
      int page = request.mandatoryParamAsInt(Param.PAGE);
      int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
      query.setPage(page, pageSize);

      ChangelogLoader.Changelog changelog = changelogLoader.load(dbSession, query);
      writeResponse(response.newJsonWriter(), page, pageSize, changelog);
    }
  }

  private static void writeResponse(JsonWriter json, int page, int pageSize, ChangelogLoader.Changelog changelog) {
    json.beginObject();
    json.prop("total", changelog.getTotal());
    json.prop(Param.PAGE, page);
    json.prop(Param.PAGE_SIZE, pageSize);
    json.name("events").beginArray();
    for (ChangelogLoader.Change change : changelog.getChanges()) {
      json.beginObject()
        .prop("date", DateUtils.formatDateTime(change.getCreatedAt()))
        .prop("authorLogin", change.getUserLogin())
        .prop("authorName", change.getUserName())
        .prop("action", change.getType())
        .prop("ruleKey", change.getRuleKey() == null ? null : change.getRuleKey().toString())
        .prop("ruleName", change.getRuleName());
      writeParameters(json, change);
      json.endObject();
    }
    json.endArray();
    json.endObject().close();
  }

  private static void writeParameters(JsonWriter json, ChangelogLoader.Change change) {
    json.name("params").beginObject()
      .prop("severity", change.getSeverity());
    for (Map.Entry<String, String> param : change.getParams().entrySet()) {
      json.prop(param.getKey(), param.getValue());
    }
    json.endObject();
  }

}
