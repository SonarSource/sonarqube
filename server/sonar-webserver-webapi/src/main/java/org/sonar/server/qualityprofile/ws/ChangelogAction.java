/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleImpactChangeDto;
import org.sonar.db.user.UserDto;

import static java.lang.String.format;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.qualityprofile.ws.QProfileChangelogFilterMode.MQR;
import static org.sonar.server.qualityprofile.ws.QProfileChangelogFilterMode.STANDARD;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_FILTER_MODE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TO;

public class ChangelogAction implements QProfileWsAction {

  private final QProfileWsSupport wsSupport;
  private final Languages languages;
  private final DbClient dbClient;

  public ChangelogAction(QProfileWsSupport wsSupport, Languages languages, DbClient dbClient) {
    this.wsSupport = wsSupport;
    this.languages = languages;
    this.dbClient = dbClient;
  }

  @Override
  public void define(NewController context) {
    NewAction wsAction = context.createAction("changelog")
      .setSince("5.2")
      .setDescription("Get the history of changes on a quality profile: rule activation/deactivation, change in " +
        "parameters/severity/impacts. " +
        "Events are ordered by date in descending order (most recent first).")
      .setChangelog(
        new Change("9.8", "response fields 'total', 's', 'ps' have been deprecated, please use 'paging' object instead"),
        new Change("9.8", "The field 'paging' has been added to the response"),
        new Change("10.3", "Added fields 'cleanCodeAttributeCategory', 'impacts' to response"),
        new Change("10.3", "Added fields 'oldCleanCodeAttribute', 'newCleanCodeAttribute', 'oldCleanCodeAttributeCategory', " +
          "'newCleanCodeAttributeCategory' and 'impactChanges' to 'params' section of response"),
        new Change("10.3", "Added field 'sonarQubeVersion' to 'params' section of response"),
        new Change("10.8", format("Added parameter '%s'", PARAM_FILTER_MODE)),
        new Change("10.8", format("Possible values '%s' and '%s' for response field 'severity' of 'impacts' have been added", INFO.name()
          , BLOCKER.name())),
        new Change("2025.1", "Added field 'prioritizedRule' to 'params' section of response"))
      .setHandler(this)
      .setResponseExample(getClass().getResource("changelog-example.json"));

    QProfileReference.defineParams(wsAction, languages);

    wsAction.addPagingParams(50, MAX_PAGE_SIZE);

    wsAction.createParam(PARAM_FILTER_MODE)
      .setDescription(format("If specified, will return changelog events related to %s or %s mode. " +
        "If not specified, all the events are returned", MQR, STANDARD))
      .setRequired(false)
      .setPossibleValues(QProfileChangelogFilterMode.values())
      .setSince("10.8")
      .setExampleValue(MQR);

    wsAction.createParam(PARAM_SINCE)
      .setDescription("Start date for the changelog (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    wsAction.createParam(PARAM_TO)
      .setDescription("End date for the changelog (exclusive, strictly before). <br>" +
        "Either a date (server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    QProfileReference reference = QProfileReference.fromName(request);
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

      query.setFilterMode(request.param(PARAM_FILTER_MODE));
      int total = dbClient.qProfileChangeDao().countByQuery(dbSession, query);

      List<QProfileChangeDto> changelogs = load(dbSession, query);
      Map<String, UserDto> usersByUuid = getUsersByUserUuid(dbSession, changelogs);
      Map<String, RuleDto> rulesByRuleIds = getRulesByRuleUuids(dbSession, changelogs);
      writeResponse(response.newJsonWriter(), total, page, pageSize, changelogs, usersByUuid, rulesByRuleIds);
    }
  }

  private Map<String, UserDto> getUsersByUserUuid(DbSession dbSession, List<QProfileChangeDto> changes) {
    Set<String> userUuids = changes.stream()
      .map(QProfileChangeDto::getUserUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    return dbClient.userDao()
      .selectByUuids(dbSession, userUuids)
      .stream()
      .collect(Collectors.toMap(UserDto::getUuid, Function.identity()));
  }

  private Map<String, RuleDto> getRulesByRuleUuids(DbSession dbSession, List<QProfileChangeDto> changes) {
    Set<String> ruleUuids = changes.stream()
      .map(ChangelogAction::extractRuleUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    return dbClient.ruleDao()
      .selectByUuids(dbSession, Lists.newArrayList(ruleUuids))
      .stream()
      .collect(Collectors.toMap(RuleDto::getUuid, Function.identity()));
  }

  private static String extractRuleUuid(QProfileChangeDto change) {
    if (change.getRuleChange() != null) {
      return change.getRuleChange().getRuleUuid();
    }
    return change.getDataAsMap().get("ruleUuid");
  }

  private static void writeResponse(JsonWriter json, int total, int page, int pageSize, List<QProfileChangeDto> changelogs,
    Map<String, UserDto> usersByUuid, Map<String, RuleDto> rulesByRuleUuids) {
    json.beginObject();
    writePaging(json, total, page, pageSize);
    json.name("paging").beginObject()
      .prop("pageIndex", page)
      .prop("pageSize", pageSize)
      .prop("total", total)
      .endObject();
    json.name("events").beginArray();
    changelogs.forEach(change -> {
      JsonWriter changeWriter = json.beginObject();
      changeWriter
        .prop("date", DateUtils.formatDateTime(change.getCreatedAt()))
        .prop("sonarQubeVersion", change.getSqVersion())
        .prop("action", change.getChangeType());
      UserDto user = usersByUuid.get(change.getUserUuid());
      if (user != null) {
        changeWriter
          .prop("authorLogin", user.getLogin())
          .prop("authorName", user.getName());
      }
      RuleDto rule = rulesByRuleUuids.get(extractRuleUuid(change));
      if (rule != null) {
        changeWriter
          .prop("ruleKey", rule.getKey().toString())
          .prop("ruleName", rule.getName());

        if (rule.getCleanCodeAttribute() != null) {
          changeWriter
            .prop("cleanCodeAttributeCategory", rule.getCleanCodeAttribute().getAttributeCategory().toString());
        }
        changeWriter
          .name("impacts")
          .beginArray();
        for (ImpactDto impact : rule.getDefaultImpacts()) {
          changeWriter
            .beginObject()
            .prop("softwareQuality", impact.getSoftwareQuality().toString())
            .prop("severity", impact.getSeverity().toString())
            .endObject();
        }

        changeWriter.endArray();
      }
      writeChanges(json, change);
      json.endObject();
    });
    json.endArray();
    json.endObject().close();
  }

  private static void writeChanges(JsonWriter json, QProfileChangeDto change) {
    json.name("params").beginObject()
      .prop("severity", change.getDataAsMap().get("severity"));
    json.prop("prioritizedRule", change.getDataAsMap().get("prioritizedRule"));
    change.getDataAsMap().entrySet().stream()
      .filter(entry -> entry.getKey().startsWith("param_"))
      .forEach(param -> json.prop(param.getKey().replace("param_", ""), param.getValue()));

    RuleChangeDto ruleChange = change.getRuleChange();
    if (ruleChange != null) {
      json
        .prop("oldCleanCodeAttribute", nameOrNull(ruleChange.getOldCleanCodeAttribute()))
        .prop("newCleanCodeAttribute", nameOrNull(ruleChange.getNewCleanCodeAttribute()))
        .prop("oldCleanCodeAttributeCategory", ruleChange.getOldCleanCodeAttribute() == null ? null : nameOrNull(ruleChange.getOldCleanCodeAttribute().getAttributeCategory()))
        .prop("newCleanCodeAttributeCategory", ruleChange.getNewCleanCodeAttribute() == null ? null : nameOrNull(ruleChange.getNewCleanCodeAttribute().getAttributeCategory()));

      if (ruleChange.getRuleImpactChanges() != null) {
        json.name("impactChanges").beginArray();
        for (RuleImpactChangeDto impact : ruleChange.getRuleImpactChanges()) {
          json.beginObject()
            .prop("oldSoftwareQuality", nameOrNull(impact.getOldSoftwareQuality()))
            .prop("newSoftwareQuality", nameOrNull(impact.getNewSoftwareQuality()))
            .prop("oldSeverity", nameOrNull(impact.getOldSeverity()))
            .prop("newSeverity", nameOrNull(impact.getNewSeverity()))
            .endObject();
        }
        json.endArray();
      }
    }
    json.endObject();
  }

  private static String nameOrNull(@Nullable Enum<?> enumValue) {
    return enumValue == null ? null : enumValue.name();
  }

  /**
   * @deprecated since 9.8 - replaced by 'paging' object structure.
   */
  @Deprecated(since = "9.8")
  private static void writePaging(JsonWriter json, int total, int page, int pageSize) {
    json.prop("total", total);
    json.prop(Param.PAGE, page);
    json.prop(Param.PAGE_SIZE, pageSize);
  }

  /**
   * @return non-null list of changes, by descending order of date
   */
  public List<QProfileChangeDto> load(DbSession dbSession, QProfileChangeQuery query) {
    return dbClient.qProfileChangeDao().selectByQuery(dbSession, query);
  }
}
