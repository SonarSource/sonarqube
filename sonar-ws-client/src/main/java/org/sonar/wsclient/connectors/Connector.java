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
package org.sonar.wsclient.connectors;

import org.sonar.wsclient.services.CreateQuery;
import org.sonar.wsclient.services.DeleteQuery;
import org.sonar.wsclient.services.Query;
import org.sonar.wsclient.services.UpdateQuery;

/**
 * @since 2.1
 */
public abstract class Connector {

  /**
   * @return JSON response or null if 404 NOT FOUND error
   * @throws ConnectionException if connection error or HTTP status not in (200, 404)
   */
  public abstract String execute(Query<?> query);

  /**
   * @return JSON response or null if 404 NOT FOUND error
   * @since 2.2
   */
  public abstract String execute(CreateQuery<?> query);

  /**
   * @return JSON response or null if 404 NOT FOUND error
   * @since 2.2
   */
  public abstract String execute(DeleteQuery query);

  /**
   * @return JSON response or null if 404 NOT FOUND error
   * @since 2.6
   */
  public abstract String execute(UpdateQuery<?> query);
}
