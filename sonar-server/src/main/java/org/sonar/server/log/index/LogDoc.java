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
package org.sonar.server.log.index;

import org.sonar.core.log.Activity;
import org.sonar.core.log.Log;
import org.sonar.server.search.BaseDoc;

import java.util.Date;
import java.util.Map;

/**
 * @since 4.4
 */
public class LogDoc extends BaseDoc implements Log {

  protected LogDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public Date time() {
    return this.getField(LogNormalizer.LogFields.TIME.field());
  }

  @Override
  public String author() {
    return this.getField(LogNormalizer.LogFields.AUTHOR.field());
  }

  @Override
  public Long executionTime() {
    return this.getField(LogNormalizer.LogFields.EXECUTION.field());
  }

  @Override
  public <K extends Activity> K getActivity() {
   return null;
  }
}
