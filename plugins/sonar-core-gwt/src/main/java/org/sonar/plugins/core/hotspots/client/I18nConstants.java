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
package org.sonar.plugins.core.hotspots.client;

import com.google.gwt.core.client.GWT;

public interface I18nConstants extends com.google.gwt.i18n.client.Constants {

  static I18nConstants INSTANCE = GWT.create(I18nConstants.class);

  @DefaultStringValue("Most violated rules")
  String titleMostViolatedRules();

  @DefaultStringValue("Most violated")
  String titleMostViolatedResources();

  @DefaultStringValue("Longest unit tests")
  String titleLongestTests();

  @DefaultStringValue("Highest complexity")
  String titleMostComplexResources();

  @DefaultStringValue("Highest duplications")
  String titleMostDuplicatedResources();

  @DefaultStringValue("Highest untested lines")
  String titleLessTested();

  @DefaultStringValue("Highest average method complexity")
  String titleMostComplexMethods();

  @DefaultStringValue("Most undocumented APIs")
  String titleMostUndocumentedAPI();

  @DefaultStringValue("No measures")
  String noMeasures();

  @DefaultStringValue("Any priority")
  String anyPriority();

  @DefaultStringValue("more")
  String moreDetails();

  @DefaultStringValue("Lack of Cohesion of Methods")
  String lcom4();

  @DefaultStringValue("Response for class")
  String rfc();

  @DefaultStringValue("Highest")
  String designTitle();
}
