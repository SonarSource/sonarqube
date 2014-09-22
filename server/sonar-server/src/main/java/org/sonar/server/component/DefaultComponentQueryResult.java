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

package org.sonar.server.component;

import org.sonar.api.component.Component;
import org.sonar.api.utils.Paging;

import java.util.Collection;

public class DefaultComponentQueryResult {

  private Collection<? extends Component> components;
  private ComponentQuery query;
  private Paging paging;

  public DefaultComponentQueryResult(Collection<? extends Component> components) {
    this.components = components;
  }

  public DefaultComponentQueryResult setPaging(Paging paging) {
    this.paging = paging;
    return this;
  }

  public ComponentQuery query() {
    return query;
  }

  public DefaultComponentQueryResult setQuery(ComponentQuery query) {
    this.query = query;
    return this;
  }

  public Collection<? extends Component> components() {
    return components;
  }

  public Paging paging() {
    return paging;
  }

}
