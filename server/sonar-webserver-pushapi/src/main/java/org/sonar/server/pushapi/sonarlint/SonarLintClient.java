/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.pushapi.sonarlint;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.AsyncContext;
import org.sonar.server.pushapi.ServerPushClient;

public class SonarLintClient extends ServerPushClient {

  private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  private final Set<String> languages;
  private final Set<String> projectUuids;

  private final String userUuid;

  public SonarLintClient(AsyncContext asyncContext, Set<String> projectUuids, Set<String> languages, String userUuid) {
    super(scheduledExecutorService, asyncContext);
    this.projectUuids = projectUuids;
    this.languages = languages;
    this.userUuid = userUuid;
  }

  public Set<String> getLanguages() {
    return languages;
  }

  public Set<String> getClientProjectUuids() {
    return projectUuids;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SonarLintClient that = (SonarLintClient) o;
    return languages.equals(that.languages)
      && projectUuids.equals(that.projectUuids)
      && asyncContext.equals(that.asyncContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(languages, projectUuids);
  }

  public String getUserUuid() {
    return userUuid;
  }
}
