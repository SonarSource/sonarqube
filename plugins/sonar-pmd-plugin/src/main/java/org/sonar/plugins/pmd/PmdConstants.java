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
package org.sonar.plugins.pmd;

import org.sonar.api.CoreProperties;

public final class PmdConstants {

  public static final String REPOSITORY_KEY = CoreProperties.PMD_PLUGIN;
  public static final String REPOSITORY_NAME = "PMD";
  public static final String PLUGIN_NAME = "PMD";
  public static final String PLUGIN_KEY = CoreProperties.PMD_PLUGIN;
  public static final String XPATH_CLASS ="net.sourceforge.pmd.rules.XPathRule";
  public static final String XPATH_EXPRESSION_PARAM ="xpath";
  public static final String XPATH_MESSAGE_PARAM ="message";
}
