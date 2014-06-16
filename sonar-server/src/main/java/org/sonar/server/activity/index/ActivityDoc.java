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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.core.activity.Activity;
import org.sonar.server.search.BaseDoc;

import java.util.Date;
import java.util.Map;

/**
 * @since 4.4
 */
public class ActivityDoc extends BaseDoc implements Activity {

  protected ActivityDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public Date time() {
    return this.getField(ActivityNormalizer.LogFields.DATE.field());
  }

  @Override
  public String author() {
    return this.getField(ActivityNormalizer.LogFields.AUTHOR.field());
  }

  @Override
  public Map<String, String> details() {
    return this.getField(ActivityNormalizer.LogFields.DETAILS.field());
  }

  @Override
  public String message() {
    return this.getField(ActivityNormalizer.LogFields.MESSAGE.field());
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
