/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component.ws;

import com.google.common.io.Resources;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexQuery;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Qualifier;

import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {

  private static final String URL_PARAM_QUERY = "s";

  private static final String[] QUALIFIERS = {
    Qualifiers.VIEW,
    Qualifiers.SUBVIEW,
    Qualifiers.PROJECT,
    Qualifiers.MODULE,
    Qualifiers.FILE,
    Qualifiers.UNIT_TEST_FILE
  };

  private static final int NUMBER_OF_RESULTS_PER_QUALIFIER = 6;

  private final ComponentIndex index;

  private DbClient dbClient;

  public SuggestionsAction(DbClient dbClient, ComponentIndex index) {
    this.dbClient = dbClient;
    this.index = index;
  }

  @Override
  public void define(WebService.NewController context) {
    NewAction action = context.createAction(ACTION_SUGGESTIONS)
      .setDescription("Internal WS for the top-right search engine")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "components-example-suggestions.json"));

    action.createParam(URL_PARAM_QUERY)
      .setRequired(true)
      .setDescription("Substring of project key (minimum 2 characters)")
      .setExampleValue("sonar");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SuggestionsWsResponse searchWsResponse = doHandle(wsRequest.param(URL_PARAM_QUERY));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  SuggestionsWsResponse doHandle(String query) {
    List<Qualifier> resultsPerQualifier = getResultsOfAllQualifiers(query);

    return SuggestionsWsResponse.newBuilder()
      .addAllResults(resultsPerQualifier)
      .build();
  }

  private List<Qualifier> getResultsOfAllQualifiers(String query) {
    return Arrays
      .stream(QUALIFIERS)
      .flatMap(qualifier -> getResultsOfQualifier(query, qualifier).map(Stream::of).orElseGet(Stream::empty))
      .collect(Collectors.toList());
  }

  private Optional<Qualifier> getResultsOfQualifier(String query, String qualifier) {
    ComponentIndexQuery componentIndexQuery = new ComponentIndexQuery(query)
      .setQualifier(qualifier)
      .setLimit(NUMBER_OF_RESULTS_PER_QUALIFIER);

    List<String> uuids = searchInIndex(componentIndexQuery);
    if (uuids.isEmpty()) {
      return Optional.empty();
    }

    List<Component> results = fetchFromDatabase(uuids)
      .stream()
      .map(SuggestionsAction::dtoToComponent)
      .collect(Collectors.toList());

    Qualifier q = Qualifier.newBuilder()
      .setQ(qualifier)
      .addAllItems(results)
      .build();
    return Optional.of(q);
  }

  private List<String> searchInIndex(ComponentIndexQuery componentIndexQuery) {
    return index.search(componentIndexQuery);
  }

  private List<ComponentDto> fetchFromDatabase(List<String> uuids) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.componentDao().selectByUuids(dbSession, uuids);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static Component dtoToComponent(ComponentDto result) {
    return Component.newBuilder()
      .setKey(result.getKey())
      .setName(result.longName())
      .build();
  }

}
