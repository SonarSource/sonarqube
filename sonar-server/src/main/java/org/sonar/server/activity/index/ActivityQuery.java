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
package org.sonar.server.activity.index;

import com.google.common.collect.Lists;
import org.sonar.core.activity.Activity;

import java.util.Collection;
import java.util.Date;

/**
 * @since 4.4
 */
public class ActivityQuery {

  private Date since;
  private Date to;
  private Collection<Activity.Type> types;

  public ActivityQuery() {
    types = Lists.newArrayList();
  }

  public Date getSince() {
    return since;
  }

  public ActivityQuery setSince(Date since) {
    this.since = since;
    return this;
  }

  public Date getTo() {
    return to;
  }

  public ActivityQuery setTo(Date to) {
    this.to = to;
    return this;
  }

  public Collection<Activity.Type> getTypes() {
    return types;
  }

  public ActivityQuery setTypes(Collection<Activity.Type> types) {
    this.types = types;
    return this;
  }
}
