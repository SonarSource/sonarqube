/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.workflow;

import com.google.common.annotations.Beta;

import java.util.Map;

/**
 * @since 3.1
 */
@Beta
public interface Review {

  /**
   * This method will probably be removed in order to decrease
   * coupling with database.
   *
   * @return not-null review id (primary key of the table REVIEWS).
   */
  Long getReviewId();

  /**
   * @return not-null rule repository, for example "checkstyle"
   */
  String getRuleRepositoryKey();

  /**
   * @return not-null rule key
   */
  String getRuleKey();

  /**
   * @return not-null rule name, in English.
   */
  String getRuleName();

  boolean isSwitchedOff();

  String getMessage();

  /**
   * @return not-null properties
   */
  Map<String, String> getProperties();

  String getStatus();

  String getResolution();

  /**
   * @return not-null severity, from INFO to BLOCKER
   */
  String getSeverity();

  /**
   * @return optional line, starting from 1
   */
  Long getLine();

  /**
   * @return true if the violation has been created by an automated rule engine,
   *         false if created by an end-user.
   */
  boolean isManual();
}
