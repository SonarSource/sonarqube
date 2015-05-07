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

package org.sonar.server.activity;

import org.picocontainer.Startable;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.Paging;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.QProfileActivity;
import org.sonar.server.qualityprofile.QProfileActivityQuery;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.search.Result;
import org.sonar.server.util.RubyUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @deprecated in 4.4 because Ruby on Rails is deprecated too !
 */
@Deprecated
@ServerSide
public class RubyQProfileActivityService implements Startable {

  private final QProfileService service;

  public RubyQProfileActivityService(QProfileService service) {
    this.service = service;
  }

  /**
   * Used in profiles_controller.rb
   */
  public QProfileActivityResult search(Map<String, Object> params) {
    QProfileActivityQuery query = new QProfileActivityQuery();

    query.setQprofileKey((String) params.get("profileKey"));
    Date since = RubyUtils.toDate(params.get("since"));
    if (since != null) {
      query.setSince(since);
    }
    Date to = RubyUtils.toDate(params.get("to"));
    if (to != null) {
      query.setTo(to);
    }

    SearchOptions options = new SearchOptions();
    Integer page = RubyUtils.toInteger(params.get("p"));
    int pageIndex = page != null ? page : 1;
    options.setPage(pageIndex, 50);

    Result<QProfileActivity> result = service.searchActivities(query, options);
    return new QProfileActivityResult(result.getHits(), Paging.create(options.getLimit(), pageIndex, (int) result.getTotal()));
  }

  @Override
  public void start() {
    // used to force pico to instantiate the singleton at startup
  }

  @Override
  public void stop() {
    // implement startable
  }

  public static class QProfileActivityResult {

    private final List<QProfileActivity> activities;

    private final Paging paging;

    public QProfileActivityResult(List<QProfileActivity> activities, Paging paging) {
      this.activities = activities;
      this.paging = paging;
    }

    public List<QProfileActivity> activities() {
      return activities;
    }

    public Paging paging() {
      return paging;
    }

  }
}
