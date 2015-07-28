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
package org.sonar.server.view.bridge;

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.views.ViewsBridge;

/**
 * As an component of PlatformLevel4, this class is responsible for notifying shutdown to the Views plugin when its
 * installed.
 */
public class ViewsStopper implements Startable {
  private static final Logger LOGGER = Loggers.get(ViewsStopper.class);

  private final ComponentContainer platformContainer;

  public ViewsStopper(ComponentContainer platformContainer) {
    this.platformContainer = platformContainer;
  }

  @Override
  public void start() {
    // nothing to do, Views plugins is started by ViewsBootstrap
  }

  @Override
  public void stop() {
    ViewsBridge viewsBridge = platformContainer.getComponentByType(ViewsBridge.class);
    if (viewsBridge != null) {
      Profiler profiler = Profiler.create(LOGGER).startInfo("Stopping views");
      viewsBridge.stopViews();
      profiler.stopInfo();
    }
  }
}
