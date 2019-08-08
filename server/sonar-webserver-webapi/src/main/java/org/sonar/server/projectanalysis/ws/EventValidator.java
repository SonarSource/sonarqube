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
package org.sonar.server.projectanalysis.ws;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.EventCategory.fromLabel;

class EventValidator {
  private static final Set<String> AUTHORIZED_CATEGORIES = ImmutableSet.of(VERSION.name(), OTHER.name());
  private static final String AUTHORIZED_CATEGORIES_INLINED = Joiner.on(", ").join(AUTHORIZED_CATEGORIES);

  private EventValidator() {
    // prevent instantiation
  }

  static Consumer<EventDto> checkModifiable() {
    return event -> checkArgument(AUTHORIZED_CATEGORIES.contains(fromLabel(event.getCategory()).name()),
      "Event of category '%s' cannot be modified. Authorized categories: %s",
      EventCategory.fromLabel(event.getCategory()), AUTHORIZED_CATEGORIES_INLINED);
  }

  static void checkVersionName(EventCategory category, @Nullable String name) {
    checkVersionName(category.getLabel(), name);
  }

  static void checkVersionName(@Nullable String category, @Nullable String name) {
    if (VERSION.getLabel().equals(category) && name != null) {
      // check against max version length defined on SnapshotDto to enforce consistency between version events and snapshot versions
      checkArgument(name.length() <= SnapshotDto.MAX_VERSION_LENGTH,
        "Event name length (%s) is longer than the maximum authorized (%s). '%s' was provided.", name.length(), SnapshotDto.MAX_VERSION_LENGTH, name);
    }
  }
}
