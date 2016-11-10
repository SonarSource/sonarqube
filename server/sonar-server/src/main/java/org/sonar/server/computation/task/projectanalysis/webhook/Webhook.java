/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.webhook;

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class Webhook {

  private final String componentUuid;
  private final String ceTaskUuid;
  private final String name;
  private final String url;

  public Webhook(String componentUuid, String ceTaskUuid, String name, String url) {
    this.componentUuid = requireNonNull(componentUuid);
    this.ceTaskUuid = requireNonNull(ceTaskUuid);
    this.name = requireNonNull(name);
    this.url = requireNonNull(url);
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public String getCeTaskUuid() {
    return ceTaskUuid;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }
}
