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
package org.sonar.api.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotation used to specify which measures should be available on a snapshot to be able to display a view (page, tab, ...).
 * It is possible to give a list of mandatory measures (= if one is not available, the view is not displayed) and/or a list of
 * needed measures (only one of them needs to be available). The measures are specified using the metric keys.
 * </p>
 * <p>
 * Example: the DesignPage absolutely requires the "dsm" measure to be fed in order to be displayed, whatever the language.
 * The class will define a <code>@RequiredMeasures(allOf={"dsm"})</code> annotation.
 * </p>
 * 
 * @since 3.0
 * @deprecated in 4.5. Not supported anymore in source viewer as Ruby on Rails is being dropped.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface RequiredMeasures {

  /**
   * Lists all the measures that must absolutely to be available on the snapshot in order to display the view.
   * @return the list of mandatory measures, identified by their metric key
   */
  String[] allOf() default {};

  /**
   * Lists all needed measures required to display the view. If only one of them is available on the snapshot, then the view 
   * is displayed.
   * @return the list of needed measures, identified by their metric key
   */
  String[] anyOf() default {};

}
