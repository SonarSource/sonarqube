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
import org.sonar.api.ServerComponent;
import org.sonar.server.qualityprofile.QProfileActivity;
import org.sonar.server.qualityprofile.QProfileActivityQuery;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.util.RubyUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @deprecated in 4.4 because Ruby on Rails is deprecated too !
 */
@Deprecated
public class RubyActivityService implements ServerComponent, Startable {

  private final QProfileService service;

  public RubyActivityService(QProfileService service) {
    this.service = service;
  }

  /**
   * Used in profiles_controller.rb
   */
  public List<QProfileActivity> search(Map<String, Object> params) {
    QProfileActivityQuery query = new QProfileActivityQuery();
    List<String> profileKeys = RubyUtils.toStrings(params.get("profileKeys"));
    if (profileKeys != null) {
      query.setQprofileKeys(profileKeys);
    }
    Date since = RubyUtils.toDate(params.get("since"));
    if (since != null) {
      query.setSince(since);
    }
    Date to = RubyUtils.toDate(params.get("to"));
    if (to != null) {
      query.setTo(to);
    }
    return service.findActivities(query, new QueryOptions().setMaxLimit());
  }

  @Override
  public void start() {
    // used to force pico to instantiate the singleton at startup
  }

  @Override
  public void stop() {
    // implement startable
  }
}
