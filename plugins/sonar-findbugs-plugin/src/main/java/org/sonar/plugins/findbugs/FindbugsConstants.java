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
package org.sonar.plugins.findbugs;

import org.sonar.api.CoreProperties;

public final class FindbugsConstants {

  public static final String REPOSITORY_KEY = CoreProperties.FINDBUGS_PLUGIN;
  public static final String REPOSITORY_NAME = "Findbugs";
  public static final String PLUGIN_NAME = "Findbugs";
  public static final String PLUGIN_KEY = CoreProperties.FINDBUGS_PLUGIN;

  /**
   * @since 2.4
   */
  public static final String GENERATE_XML_KEY = "sonar.findbugs.generateXml";
  public static final boolean GENERATE_XML_DEFAULT_VALUE = true; // TODO should be false
}
