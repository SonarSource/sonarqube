/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;

import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TO;

public class ChangelogAction implements QProfileWsAction {

  private final QProfileWsSupport wsSupport;
  private final Languages languages;
  private DbClient dbClient;

  public ChangelogAction(QProfileWsSupport wsSupport, Languages languages, DbClient dbClient) {
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
      .setDescription("Start date for the changelog. <br>" +
        "Either a date (server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    wsAction.createParam(PARAM_TO)
      .setDescription("End date for the changelog. <br>" +
        "Either a date (server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");
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

      int total = dbClient.qProfileChangeDao().countByQuery(dbSession, query);

      List<Change> changelogs = load(dbSession, query);
      Map<String, UserDto> usersByUuid = getUsersByUserUuid(dbSession, changelogs);
      Map<Integer, RuleDefinitionDto> rulesByRuleIds = getRulesByRuleIds(dbSession, changelogs);
      writeResponse(response.newJsonWriter(), total, page, pageSize, changelogs, usersByUuid, rulesByRuleIds);
    }
  }

  private Map<String, UserDto> getUsersByUserUuid(DbSession dbSession, List<Change> changes) {
    Set<String> userUuids = changes.stream()
      .map(Change::getUserUuid)
      .filter(Objects::nonNull)
      .collect(toSet());
    return dbClient.userDao()
      .selectByUuids(dbSession, userUuids)
      .stream()
      .collect(uniqueIndex(UserDto::getUuid));
  }

  private Map<Integer, RuleDefinitionDto> getRulesByRuleIds(DbSession dbSession, List<Change> changes) {
    Set<Integer> ruleIds = changes.stream()
      .map(c -> c.ruleId)
      .filter(Objects::nonNull)
      .collect(toSet());
    return dbClient.ruleDao()
      .selectDefinitionByIds(dbSession, Lists.newArrayList(ruleIds))
      .stream()
      .collect(uniqueIndex(RuleDefinitionDto::getId));
  }

  private static void writeResponse(JsonWriter json, int total, int page, int pageSize, List<Change> changelogs,
    Map<String, UserDto> usersByUuid, Map<Integer, RuleDefinitionDto> rulesByRuleIds) {
    json.beginObject();
    json.prop("total", total);
    json.prop(Param.PAGE, page);
    json.prop(Param.PAGE_SIZE, pageSize);
    json.name("events").beginArray();
    changelogs.forEach(change -> {
      JsonWriter changeWriter = json.beginObject();
      changeWriter
        .prop("date", DateUtils.formatDateTime(change.getCreatedAt()))
        .prop("action", change.getType());
      UserDto user = usersByUuid.get(change.getUserUuid());
      if (user != null) {
        changeWriter
          .prop("authorLogin", user.getLogin())
          .prop("authorName", user.getName());
      }
      RuleDefinitionDto rule = rulesByRuleIds.get(change.getRuleId());
      if (rule != null) {
        changeWriter
          .prop("ruleKey", rule.getKey().toString())
          .prop("ruleName", rule.getName());
      }
      writeParameters(json, change);
      json.endObject();
    });
    json.endArray();
    json.endObject().close();
  }

  private static void writeParameters(JsonWriter json, Change change) {
    json.name("params").beginObject()
      .prop("severity", change.getSeverity());
    for (Map.Entry<String, String> param : change.getParams().entrySet()) {
      json.prop(param.getKey(), param.getValue());
    }
    json.endObject();
  }

  /**
   * @return non-null list of changes, by descending order of date
   */
  public List<Change> load(DbSession dbSession, QProfileChangeQuery query) {
    List<QProfileChangeDto> changeDtos = dbClient.qProfileChangeDao().selectByQuery(dbSession, query);
    return changeDtos.stream()
      .map(Change::from)
      .collect(MoreCollectors.toList(changeDtos.size()));
  }

  static class Change {
    private String key;
    private String type;
    private long at;
    private String severity;
    private String userUuid;
    private String inheritance;
    private Integer ruleId;
    private final Map<String, String> params = new HashMap<>();

    private Change() {
    }

    public String getKey() {
      return key;
    }

    @CheckForNull
    public String getSeverity() {
      return severity;
    }

    @CheckForNull
    public String getUserUuid() {
      return userUuid;
    }

    public String getType() {
      return type;
    }

    @CheckForNull
    public String getInheritance() {
      return inheritance;
    }

    @CheckForNull
    public Integer getRuleId() {
      return ruleId;
    }

    public long getCreatedAt() {
      return at;
    }

    public Map<String, String> getParams() {
      return params;
    }

    private static Change from(QProfileChangeDto dto) {
      Map<String, String> data = dto.getDataAsMap();
      Change change = new Change();
      change.key = dto.getUuid();
      change.userUuid = dto.getUserUuid();
      change.type = dto.getChangeType();
      change.at = dto.getCreatedAt();
      // see content of data in class org.sonar.server.qualityprofile.ActiveRuleChange
      change.severity = data.get("severity");
      String ruleId = data.get("ruleId");
      if (ruleId != null) {
        change.ruleId = Integer.parseInt(ruleId);
      }
      change.inheritance = data.get("inheritance");
      data.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("param_"))
        .forEach(entry -> change.params.put(entry.getKey().replace("param_", ""), entry.getValue()));
      return change;
    }
  }
}
