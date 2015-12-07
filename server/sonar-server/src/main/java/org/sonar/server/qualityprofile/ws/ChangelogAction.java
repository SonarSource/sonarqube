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

import java.util.Date;
import java.util.Map.Entry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileActivity;
import org.sonar.server.qualityprofile.QProfileActivityQuery;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.search.Result;

import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;

public class ChangelogAction implements QProfileWsAction {

  private static final String PARAM_SINCE = "since";
  private static final String PARAM_TO = "to";

  private DbClient dbClient;
  private ActivityIndex activityIndex;
  private QProfileFactory profileFactory;
  private Languages languages;

  public ChangelogAction(DbClient dbClient, ActivityIndex activityIndex, QProfileFactory profileFactory, Languages languages) {
    this.dbClient = dbClient;
    this.activityIndex = activityIndex;
    this.profileFactory = profileFactory;
    this.languages = languages;
  }

  @Override
  public void define(NewController context) {
    NewAction changelog = context.createAction("changelog")
      .setSince("5.2")
      .setDescription("Get the history of changes on a quality profile: rule activation/deactivation, change in parameters/severity. " +
        "Events are ordered by date in descending order (most recent first).")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-changelog.json"));

    QProfileIdentificationParamUtils.defineProfileParams(changelog, languages);

    changelog.addPagingParams(50, MAX_LIMIT);

    changelog.createParam(PARAM_SINCE)
      .setDescription("Start date for the changelog.")
      .setExampleValue("2011-04-25T01:15:42+0100");

    changelog.createParam(PARAM_TO)
      .setDescription("End date for the changelog.")
      .setExampleValue("2013-07-25T07:35:42+0200");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession session = dbClient.openSession(false);
    try {
      String profileKey = QProfileIdentificationParamUtils.getProfileKeyFromParameters(request, profileFactory, session);
      if (dbClient.qualityProfileDao().selectByKey(session, profileKey) == null) {
        throw new NotFoundException(String.format("Could not find a profile with key '%s'", profileKey));
      }

      QProfileActivityQuery query = new QProfileActivityQuery().setQprofileKey(profileKey);
      Date since = request.paramAsDateTime(PARAM_SINCE);
      if (since != null) {
        query.setSince(since);
      }
      Date to = request.paramAsDateTime(PARAM_TO);
      if (to != null) {
        query.setTo(to);
      }
      SearchOptions options = new SearchOptions();

      int page = request.mandatoryParamAsInt(Param.PAGE);
      options.setPage(page, request.mandatoryParamAsInt(Param.PAGE_SIZE));

      Result<QProfileActivity> result = searchActivities(query, options);
      writeResponse(response.newJsonWriter(), result, forPageIndex(page).withPageSize(options.getLimit()).andTotal((int) result.getTotal()));
    } finally {
      session.close();
    }
  }

  private Result<QProfileActivity> searchActivities(QProfileActivityQuery query, SearchOptions options) {
    DbSession session = dbClient.openSession(false);
    try {
      SearchResponse response = activityIndex.doSearch(query, options);
      Result<QProfileActivity> result = new Result<>(response);
      for (SearchHit hit : response.getHits().getHits()) {
        QProfileActivity profileActivity = new QProfileActivity(hit.getSource());
        RuleDto ruleDto = dbClient.deprecatedRuleDao().getNullableByKey(session, profileActivity.ruleKey());
        profileActivity.ruleName(ruleDto != null ? ruleDto.getName() : null);

        String login = profileActivity.getLogin();
        if (login != null) {
          UserDto user = dbClient.userDao().selectActiveUserByLogin(session, login);
          profileActivity.authorName(user != null ? user.getName() : null);
        }
        result.getHits().add(profileActivity);
      }
      return result;
    } finally {
      session.close();
    }
  }

  private void writeResponse(JsonWriter json, Result<QProfileActivity> result, Paging paging) {
    json.beginObject();
    json.prop("total", result.getTotal());
    json.prop(Param.PAGE, paging.pageIndex());
    json.prop(Param.PAGE_SIZE, paging.pageSize());
    json.name("events").beginArray();
    for (QProfileActivity event : result.getHits()) {
      json.beginObject()
        .prop("date", DateUtils.formatDateTime(event.getCreatedAt()))
        .prop("authorLogin", event.getLogin())
        .prop("authorName", event.authorName())
        .prop("action", event.getAction())
        .prop("ruleKey", event.ruleKey().toString())
        .prop("ruleName", event.ruleName());
      writeParameters(json, event);
      json.endObject();
    }
    json.endArray();
    json.endObject().close();
  }

  private void writeParameters(JsonWriter json, QProfileActivity event) {
    json.name("params").beginObject()
      .prop("severity", event.severity());
    for (Entry<String, String> param : event.parameters().entrySet()) {
      json.prop(param.getKey(), param.getValue());
    }
    json.endObject();
  }

}
