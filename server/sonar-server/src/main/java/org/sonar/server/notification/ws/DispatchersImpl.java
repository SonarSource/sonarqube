/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.notification.ws;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.process.ProcessProperties;
import org.sonar.server.event.NewAlerts;
import org.sonar.server.issue.notification.DoNotFixNotificationDispatcher;
import org.sonar.server.issue.notification.NewIssuesNotificationDispatcher;
import org.sonar.server.notification.NotificationCenter;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;

public class DispatchersImpl implements Dispatchers, Startable {

  private static final Set<String> GLOBAL_DISPATCHERS_TO_IGNORE_ON_SONAR_CLOUD = ImmutableSet.of(
    NewAlerts.KEY,
    DoNotFixNotificationDispatcher.KEY,
    NewIssuesNotificationDispatcher.KEY);

  private final NotificationCenter notificationCenter;
  private final Configuration configuration;

  private List<String> projectDispatchers;
  private List<String> globalDispatchers;

  public DispatchersImpl(NotificationCenter notificationCenter, Configuration configuration) {
    this.notificationCenter = notificationCenter;
    this.configuration = configuration;
  }

  @Override
  public List<String> getGlobalDispatchers() {
    return globalDispatchers;
  }

  @Override
  public List<String> getProjectDispatchers() {
    return projectDispatchers;
  }

  @Override
  public void start() {
    boolean isOnSonarCloud = configuration.getBoolean(ProcessProperties.Property.SONARCLOUD_ENABLED.getKey()).orElse(false);
    this.globalDispatchers = notificationCenter.getDispatcherKeysForProperty(GLOBAL_NOTIFICATION, "true")
      .stream()
      .filter(filterDispatcherForSonarCloud(isOnSonarCloud))
      .sorted()
      .collect(toList());
    this.projectDispatchers = notificationCenter.getDispatcherKeysForProperty(PER_PROJECT_NOTIFICATION, "true")
      .stream()
      .sorted()
      .collect(toList());
  }

  private static Predicate<String> filterDispatcherForSonarCloud(boolean isOnSonarCloud) {
    return dispatcher -> !(isOnSonarCloud && GLOBAL_DISPATCHERS_TO_IGNORE_ON_SONAR_CLOUD.contains(dispatcher));
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
