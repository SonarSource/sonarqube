/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.web;

import java.util.Arrays;
import org.sonar.api.Startable;
import org.sonar.api.web.HttpFilter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 3.5
 */
public class RegisterServletFilters implements Startable {
  private final HttpFilter[] httpFilters;

  @Autowired(required = false)
  public RegisterServletFilters(HttpFilter[] httpFilters) {
    this.httpFilters = httpFilters;
  }

  @Autowired(required = false)
  public RegisterServletFilters() {
    this(new HttpFilter[0]);
  }

  @Override
  public void start() {
    MasterServletFilter masterServletFilter = MasterServletFilter.getInstance();
    if (masterServletFilter != null) {
      // Probably a database upgrade. MasterSlaveFilter was instantiated by the servlet container
      // while spring was not completely up.
      // See https://jira.sonarsource.com/browse/SONAR-3612
      masterServletFilter.initHttpFilters(Arrays.asList(httpFilters));
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
