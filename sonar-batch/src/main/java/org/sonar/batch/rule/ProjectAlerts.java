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
package org.sonar.batch.rule;

import com.google.common.collect.Lists;
import org.sonar.api.BatchComponent;
import org.sonar.api.profiles.Alert;

import java.util.List;

/**
 * Lists the alerts enabled on the current project.
 */
public class ProjectAlerts implements BatchComponent {

  private final List<Alert> alerts = Lists.newArrayList();

  public ProjectAlerts() {
  }

  public void add(Alert alert) {
    alerts.add(alert);
  }

  public List<Alert> all() {
    return alerts;
  }

}
