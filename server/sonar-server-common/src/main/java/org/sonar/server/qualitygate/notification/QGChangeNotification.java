/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualitygate.notification;

import javax.annotation.CheckForNull;
import org.sonar.api.notifications.Notification;
public class QGChangeNotification extends Notification {

  public static final String FIELD_PROJECT_NAME = "projectName";
  public static final String FIELD_PROJECT_KEY = "projectKey";
  public static final String FIELD_PROJECT_ID = "projectId";
  public static final String FIELD_PROJECT_VERSION = "projectVersion";
  public static final String FIELD_ALERT_NAME = "alertName";
  public static final String FIELD_ALERT_TEXT = "alertText";
  public static final String FIELD_ALERT_LEVEL = "alertLevel";
  public static final String FIELD_PREVIOUS_ALERT_LEVEL = "previousAlertLevel";
  public static final String FIELD_IS_NEW_ALERT = "isNewAlert";
  public static final String FIELD_BRANCH = "branch";
  public static final String FIELD_IS_MAIN_BRANCH = "isMainBranch";
  public static final String FIELD_RATING_METRICS = "ratingMetrics";

  public QGChangeNotification() {
    super("alerts");
  }

  @CheckForNull
  public String getProjectKey() {
    return getFieldValue(FIELD_PROJECT_KEY);
  }
}
