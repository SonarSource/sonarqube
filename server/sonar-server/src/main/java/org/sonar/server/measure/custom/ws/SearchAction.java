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
package org.sonar.server.measure.custom.ws;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonar.server.component.ComponentFinder.ParamNames.PROJECT_ID_AND_KEY;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.measure.custom.ws.CustomMeasureValidator.checkPermissions;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SearchAction implements CustomMeasuresWsAction {

  public static final String ACTION = "search";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";

  private final DbClient dbClient;
  private final CustomMeasureJsonWriter customMeasureJsonWriter;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public SearchAction(DbClient dbClient, CustomMeasureJsonWriter customMeasureJsonWriter, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.customMeasureJsonWriter = customMeasureJsonWriter;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("List custom measures. The project id or project key must be provided.<br />" +
        "Requires 'Administer System' permission or 'Administer' permission on the project.")
      .setSince("5.2")
      .addFieldsParam(CustomMeasureJsonWriter.OPTIONAL_FIELDS)
      .addPagingParams(100, MAX_LIMIT)
      .setResponseExample(getClass().getResource("example-search.json"))
      .setHandler(this);

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectUuid = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);
    List<String> fieldsToReturn = request.paramAsStrings(WebService.Param.FIELDS);
    SearchOptions searchOptions = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(WebService.Param.PAGE),
        request.mandatoryParamAsInt(WebService.Param.PAGE_SIZE));

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByUuidOrKey(dbSession, projectUuid, projectKey, PROJECT_ID_AND_KEY);
      checkPermissions(userSession, component);
      Long lastAnalysisDateMs = searchLastSnapshotDate(dbSession, component);
      List<CustomMeasureDto> customMeasures = searchCustomMeasures(dbSession, component, searchOptions);
      int nbCustomMeasures = countTotalOfCustomMeasures(dbSession, component);
      Map<String, UserDto> usersByLogin = usersByLogin(dbSession, customMeasures);
      Map<Integer, MetricDto> metricsById = metricsById(dbSession, customMeasures);

      writeResponse(response, customMeasures, nbCustomMeasures, component, metricsById, usersByLogin, lastAnalysisDateMs, searchOptions, fieldsToReturn);
    }
  }

  @CheckForNull
  private Long searchLastSnapshotDate(DbSession dbSession, ComponentDto component) {
    Optional<SnapshotDto> lastSnapshot = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.projectUuid());

    return lastSnapshot.isPresent() ? lastSnapshot.get().getBuildDate() : null;
  }

  private int countTotalOfCustomMeasures(DbSession dbSession, ComponentDto project) {
    return dbClient.customMeasureDao().countByComponentUuid(dbSession, project.uuid());
  }

  private List<CustomMeasureDto> searchCustomMeasures(DbSession dbSession, ComponentDto project, SearchOptions searchOptions) {
    return dbClient.customMeasureDao().selectByComponentUuid(dbSession, project.uuid(), searchOptions.getOffset(), searchOptions.getLimit());
  }

  private void writeResponse(Response response, List<CustomMeasureDto> customMeasures, int nbCustomMeasures, ComponentDto project, Map<Integer, MetricDto> metricsById,
    Map<String, UserDto> usersByLogin, @Nullable Long lastAnalysisDate, SearchOptions searchOptions, List<String> fieldsToReturn) {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    customMeasureJsonWriter.write(json, customMeasures, project, metricsById, usersByLogin, lastAnalysisDate, fieldsToReturn);
    searchOptions.writeJson(json, nbCustomMeasures);
    json.endObject();
    json.close();
  }

  private Map<Integer, MetricDto> metricsById(DbSession dbSession, List<CustomMeasureDto> customMeasures) {
    List<MetricDto> metrics = dbClient.metricDao().selectByIds(dbSession, newHashSet(Lists.transform(customMeasures, CustomMeasureToMetricIdFunction.INSTANCE)));
    return Maps.uniqueIndex(metrics, MetricToIdFunction.INSTANCE);
  }

  private Map<String, UserDto> usersByLogin(DbSession dbSession, List<CustomMeasureDto> customMeasures) {
    List<String> logins = FluentIterable.from(customMeasures)
      .transform(CustomMeasureToUserLoginFunction.INSTANCE)
      .toList();
    List<UserDto> userDtos = dbClient.userDao().selectByLogins(dbSession, logins);
    return FluentIterable.from(userDtos).uniqueIndex(UserDtoToLogin.INSTANCE);
  }

  private enum CustomMeasureToUserLoginFunction implements Function<CustomMeasureDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull CustomMeasureDto customMeasure) {
      return customMeasure.getUserLogin();
    }
  }

  private enum UserDtoToLogin implements Function<UserDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull UserDto input) {
      return input.getLogin();
    }
  }

  private enum CustomMeasureToMetricIdFunction implements Function<CustomMeasureDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull CustomMeasureDto customMeasure) {
      return customMeasure.getMetricId();
    }
  }

  private enum MetricToIdFunction implements Function<MetricDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull MetricDto metric) {
      return metric.getId();
    }
  }
}
