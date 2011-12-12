/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to specify a widget associated to a dashboard template. It must be nested inside a {@link DashboardWidgets} annotation.
 * It can contain {@link WidgetProperties} annotation to modify the widget default parameters.
 * 
 * @since 2.13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DashboardWidget {

  /**
   * ID of the widget.
   * 
   * @return the id
   */
  String id();

  /**
   * The index of the column for this widget.
   * 
   * @return the column index
   */
  int columnIndex();

  /**
   * The index of the row for this widget.
   * 
   * @return the row index
   */
  int rowIndex();

  /**
   * The widget specific properties.
   * 
   * @return the widget properties
   */
  WidgetProperty[] properties() default {};

}
