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
package org.sonar.api.batch;

import org.sonar.api.BatchSide;
import org.sonar.api.measures.Measure;

import java.util.List;

/**
 * The TimeMachine component
 *
 * @since 1.10
 * @deprecated since 5.1 ability to access previous data from batch side will soon be removed
 */
@Deprecated
@RequiresDB
@BatchSide
public interface TimeMachine {

  /**
   * Past measures, sorted by date. Returns all fields.
   * <p/>
   * <p>Measures of current analysis are not included.</p>
   */
  List<Measure> getMeasures(TimeMachineQuery query);

  /**
   * Return an empty list since 5.2. See https://jira.codehaus.org/browse/SONAR-6392
   */
  List<Object[]> getMeasuresFields(TimeMachineQuery query);

}
