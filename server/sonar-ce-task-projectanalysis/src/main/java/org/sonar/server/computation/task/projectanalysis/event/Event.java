/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.event;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;


@Immutable
public class Event {
  private final String name;
  private final Category category;
  @Nullable
  private final String data;
  @Nullable
  private final String description;

  private Event(String name, Category category, @Nullable String data, @Nullable String description) {
    this.name = requireNonNull(name);
    this.category = requireNonNull(category);
    this.data = data;
    this.description = description;
  }

  public static Event createAlert(String name, @Nullable String data, @Nullable String description) {
    return new Event(name, Category.ALERT, data, description);
  }

  public static Event createProfile(String name, @Nullable String data, @Nullable String description) {
    return new Event(name, Category.PROFILE, data, description);
  }

  public String getName() {
    return name;
  }

  public Category getCategory() {
    return category;
  }

  public String getData() {
    return data;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Event event = (Event) o;
    return Objects.equals(name, event.name) &&
        Objects.equals(category, event.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, category);
  }

  public enum Category {
    ALERT, PROFILE
  }

}
