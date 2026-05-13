/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.events;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for the {@link ObjectMapper} used to serialize and deserialize unified events
 * exchanged between the Web process ({@link CeQueueEventForwarder}) and the Compute Engine
 * ({@link CeEventTaskProcessor}).
 *
 * <p>Centralizing mapper creation here ensures both sides of the round-trip always use
 * identical configuration, so any future change (e.g. registering a Jackson module or
 * adjusting null-handling) only needs to be made in one place.
 */
final class EventsObjectMapper {

  private EventsObjectMapper() {
    // static factory — not instantiated
  }

  static ObjectMapper create() {
    return new ObjectMapper();
  }
}
