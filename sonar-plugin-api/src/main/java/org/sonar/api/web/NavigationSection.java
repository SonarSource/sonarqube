/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.api.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Page is displayed only in listed sections. This annotation is ignored on Widgets.
 * 
 * @since 1.11
 * @deprecated since 6.3 see {@link org.sonar.api.web.page.PageDefinition}. This class is ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface NavigationSection {

  String HOME = "home";
  String RESOURCE = "resource";

  /**
   * Support removed in 5.1. See https://jira.sonarsource.com/browse/SONAR-6016.
   * @deprecated in 4.5, as it costs too much to maintain and update.
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-5321">SONAR-5321</a>
   */
  @Deprecated
  String RESOURCE_TAB = "resource_tab";

  String CONFIGURATION = "configuration";

  /**
   * Only Ruby and rails application. See "Extend Web Application" section of https://redirect.sonarsource.com/doc/extension-guide.html.
   * Use the resource parameter in order to get the current resource.
   *
   * @since 3.6
   */
  String RESOURCE_CONFIGURATION = "resource_configuration";

  String[] value() default { HOME };

}
