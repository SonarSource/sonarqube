/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Property value can be set in different ways :
 * <ul>
 * <li>System property</li>
 * <li>Maven command-line (-Dfoo=bar)</li>
 * <li>Maven pom.xml (element <properties>)</li>
 * <li>Maven settings.xml</li>
 * <li>Sonar web interface</li>
 * </ul>
 * <p/>
 * Value is accessible in batch extensions via the Configuration object of class <code>org.sonar.api.resources.Project</code>
 * (see method <code>getConfiguration()</code>).
 * <p/>
 * <p><strong>Must be used in <code>org.sonar.api.Plugin</code> classes only.</strong></p>
 *
 * @since 1.10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Property {

  /**
   * Unique key within all plugins. It's recommended to prefix the key by 'sonar.' and the plugin name. Examples :
   * 'sonar.cobertura.reportPath' and 'sonar.cpd.minimumTokens'.
   */
  String key();

  String defaultValue() default "";

  String name();

  String description() default "";

  /**
   * Is the property displayed in projet settings page ?
   */
  boolean project() default false;

  /**
   * Is the property displayed in module settings page ? A module is a maven sub-project.
   */
  boolean module() default false;

  /**
   * Is the property displayed in global settings page ?
   */
  boolean global() default true;
}
