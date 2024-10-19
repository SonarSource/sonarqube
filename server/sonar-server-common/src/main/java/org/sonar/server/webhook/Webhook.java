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
package org.sonar.server.webhook;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

@Immutable
public class Webhook {

  private final String uuid;
  private final String projectUuid;
  @Nullable
  private final String ceTaskUuid;
  @Nullable
  private final String analysisUuid;
  private final String name;
  private final String url;
  @Nullable
  private final String secret;

  public Webhook(String uuid, String projectUuid, @Nullable String ceTaskUuid,
    @Nullable String analysisUuid, String name, String url, @Nullable String secret) {
    this.uuid = uuid;
    this.projectUuid = requireNonNull(projectUuid);
    this.ceTaskUuid = ceTaskUuid;
    this.analysisUuid = analysisUuid;
    this.name = requireNonNull(name);
    this.url = requireNonNull(url);
    this.secret = secret;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public Optional<String> getCeTaskUuid() {
    return ofNullable(ceTaskUuid);
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getUuid() {
    return uuid;
  }

  public Optional<String> getAnalysisUuid() {
    return ofNullable(analysisUuid);
  }

  public Optional<String> getSecret() {
    return ofNullable(secret);
  }
}
