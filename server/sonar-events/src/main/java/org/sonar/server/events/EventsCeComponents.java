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
import org.sonarsource.sonarqube.events.server.EventsServerCeConfiguration;

/**
 * Spring components for unified event consuming in the Compute Engine process.
 *
 * <p>Register these components in the CE Spring context via
 * {@code level4Container.add(toArray(EventsCeComponents.components()))} in
 * {@code ComputeEngineContainerImpl}.
 *
 * <p>{@link EventsServerCeConfiguration} wires {@code EventDispatcher} (COMPUTE_ENGINE process).
 * {@link CeEventTaskProcessor} picks up forwarded events from the CE queue and
 * dispatches them to handlers declaring {@code ExecutingProcess.COMPUTE_ENGINE}.
 *
 * <p>Feature-module {@link org.sonarsource.sonarqube.events.api.TaskHandler} beans are
 * registered separately and auto-collected by Spring.
 */
public class EventsCeComponents {

  private EventsCeComponents() {
    // static factory — not instantiated
  }

  public static List<Class<?>> components() {
    return List.of(
      EventsServerCeConfiguration.class,
      CeEventTaskProcessor.class
    );
  }
}
