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

package org.sonarqube.ws.client.projectanalysis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class UpdateEventRequest {
  private final String event;
  private final String name;
  private final String description;

  public UpdateEventRequest(String event, @Nullable String name, @Nullable String description) {
    checkArgument(name != null || description != null, "Name or description is required");
    this.event = requireNonNull(event, "Event key is required");
    this.name = name;
    this.description = description;
  }

  public String getEvent() {
    return event;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }
}
