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
package org.sonar.server.events;

import java.util.List;
import org.sonarsource.sonarqube.events.server.EventsServerWebConfiguration;

/**
 * Spring components for unified event publishing in the Web process.
 *
 * <p>Register these components in the Web Spring context via
 * {@code addAll(EventsWebComponents.components())} in {@code PlatformLevel4}.
 *
 * <p>{@link EventsServerWebConfiguration} wires {@code ServerEventSourceBuilder},
 * {@code EventDispatcher} (SERVER process), and {@code ServerEventAsyncClient}.
 * {@link CeQueueEventForwarder} is injected as the {@code CrossProcessEventForwarder}
 * for CE-targeted event forwarding.
 *
 * <p>Feature-module {@link org.sonarsource.sonarqube.events.api.TaskHandler} beans are
 * registered separately and auto-collected by Spring.
 */
public class EventsWebComponents {

  private EventsWebComponents() {
    // static factory — not instantiated
  }

  public static List<Class<?>> components() {
    return List.of(
      EventsServerWebConfiguration.class,
      CeQueueEventForwarder.class
    );
  }
}
