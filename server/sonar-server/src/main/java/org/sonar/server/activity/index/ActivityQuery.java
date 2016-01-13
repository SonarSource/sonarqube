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
package org.sonar.server.activity.index;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActivityQuery {

  private Date since;
  private Date to;
  private final Collection<String> types = new ArrayList<>();
  private final Map<String, Object> dataOrFilters = new LinkedHashMap<>();

  @CheckForNull
  public Date getSince() {
    return since;
  }

  public ActivityQuery setSince(@Nullable Date since) {
    this.since = since;
    return this;
  }

  @CheckForNull
  public Date getTo() {
    return to;
  }

  public ActivityQuery setTo(@Nullable Date to) {
    this.to = to;
    return this;
  }

  public Collection<String> getTypes() {
    return types;
  }

  public ActivityQuery setTypes(@Nullable Collection<String> types) {
    this.types.clear();
    if (types != null) {
      this.types.addAll(types);
    }
    return this;
  }

  public Map<String, Object> getDataOrFilters() {
    return dataOrFilters;
  }

  public ActivityQuery addDataOrFilter(String dataKey, Object dataValue) {
    dataOrFilters.put(dataKey, dataValue);
    return this;
  }
}
