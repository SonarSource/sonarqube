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

package org.sonar.server.issue.filter;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueFilterDao;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.db.issue.IssueFilterFavouriteDto;
import org.sonar.server.user.UserSession;

public class SearchAction implements IssueFilterWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public SearchAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search");
    action
      .setDescription("List of current user issue filters and shared issue filters.")
      .setHandler(this)
      .setSince("5.2")
      .setResponseExample(getClass().getResource("search-example.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Set<IssueFilterDto> issueFilters = searchIssueFilters(dbSession);
      Map<Long, IssueFilterFavouriteDto> userFavouritesByFilterId = searchUserFavouritesByFilterId(dbSession);
      writeResponse(response, issueFilters, userFavouritesByFilterId);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void writeResponse(Response response, Set<IssueFilterDto> issueFilters, Map<Long, IssueFilterFavouriteDto> userFavouritesByFilterId) {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    json.name("issueFilters").beginArray();
    for (IssueFilterDto issueFilter : issueFilters) {
      IssueFilterJsonWriter.write(json, new IssueFilterWithFavorite(issueFilter, isFavourite(issueFilter, userFavouritesByFilterId)), userSession);
    }
    json.endArray();

    json.endObject();
    json.close();
  }

  private static boolean isFavourite(IssueFilterDto issueFilter, Map<Long, IssueFilterFavouriteDto> userFavouritesByFilterId) {
    return userFavouritesByFilterId.get(issueFilter.getId()) != null;
  }

  /**
   * @return all the current user issue filters and all the shared filters
   */
  private Set<IssueFilterDto> searchIssueFilters(DbSession dbSession) {
    IssueFilterDao issueFilterDao = dbClient.issueFilterDao();

    List<IssueFilterDto> filters = issueFilterDao.selectByUser(dbSession, userSession.getLogin());
    List<IssueFilterDto> sharedFilters = issueFilterDao.selectSharedFilters(dbSession);
    filters.addAll(sharedFilters);

    return FluentIterable.from(filters).toSortedSet(IssueFilterDtoIdComparator.INSTANCE);
  }

  private Map<Long, IssueFilterFavouriteDto> searchUserFavouritesByFilterId(DbSession dbSession) {
    List<IssueFilterFavouriteDto> favouriteFilters = dbClient.issueFilterFavouriteDao().selectByUser(dbSession, userSession.getLogin());
    return Maps.uniqueIndex(favouriteFilters, IssueFilterFavouriteDToIssueFilterId.INSTANCE);
  }

  private enum IssueFilterDtoIdComparator implements Comparator<IssueFilterDto> {
    INSTANCE;

    @Override
    public int compare(IssueFilterDto o1, IssueFilterDto o2) {
      return o1.getId().intValue() - o2.getId().intValue();
    }
  }

  private enum IssueFilterFavouriteDToIssueFilterId implements Function<IssueFilterFavouriteDto, Long> {
    INSTANCE;

    @Override
    public Long apply(@Nonnull IssueFilterFavouriteDto filterFavourite) {
      return filterFavourite.getIssueFilterId();
    }
  }
}
