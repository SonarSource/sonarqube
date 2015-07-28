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
package org.sonar.server.views;

import org.sonar.core.platform.ComponentContainer;

/**
 * Interface implemented by the Extension point exposed by the Views plugin that serves as the unique access point from
 * the whole SQ instance into the Views plugin.
 */
public interface ViewsBridge {

  /**
   * Bootstraps the Views plugin.
   *
   * @param parent the parent ComponentContainer which provides Platform components for Views to use.
   *
   * @throws IllegalStateException if called more than once
   */
  void startViews(ComponentContainer parent);

  /**
   * This method is called when Platform is shutting down.
   */
  void stopViews();

  /**
   * Triggers an update of Views tree and measures.
   *
   * @throws IllegalStateException if {@link #startViews(ComponentContainer)} has not been called
   */
  void updateViews();

}
