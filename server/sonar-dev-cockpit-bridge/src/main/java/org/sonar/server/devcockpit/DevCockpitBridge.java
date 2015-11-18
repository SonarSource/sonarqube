/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.devcockpit;

import java.util.List;
import org.sonar.core.platform.ComponentContainer;

/**
 * Interface implemented by the Extension point exposed by the Developer Cockpit plugin that serves as the unique access
 * point from the whole SQ instance into the Developer Cockpit plugin.
 */
public interface DevCockpitBridge {

  /**
   * Bootstraps the Developer Cockpit plugin.
   *
   * @param parent the parent ComponentContainer which provides Platform components for Developer Cockpit to use.
   *
   * @throws IllegalStateException if called more than once
   */
  void startDevCockpit(ComponentContainer parent);

  /**
   * This method is called when Platform is shutting down.
   */
  void stopDevCockpit();

  /**
   * Return the list of components to add to the state-full container of a Compute Engine report analysis task
   */
  List<Object> getCeComponents();

}
