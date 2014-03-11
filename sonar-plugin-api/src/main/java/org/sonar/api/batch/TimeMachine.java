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

import org.sonar.api.BatchComponent;
import org.sonar.api.measures.Measure;

import java.util.List;

/**
 * The TimeMachine extension point
 *
 * @since 1.10
 */
public interface TimeMachine extends BatchComponent {

  /**
   * Past measures, sorted by date. Returns all fields.
   * <p/>
   * <p>Measures of current analysis are not included.</p>
   */
  List<Measure> getMeasures(TimeMachineQuery query);

  /**
   * Past measures sorted by date. Return only a subset of basic fields : [date (java.util.Date), metric (org.sonar.api.measures.Metric), value (Double)].
   * <p/>
   * <p>Measures of current analysis are not included.</p>
   * <p>This method is recommended instead of getMeasures() for performance reasons. It needs less memory.</p>
   */
  List<Object[]> getMeasuresFields(TimeMachineQuery query);

}
