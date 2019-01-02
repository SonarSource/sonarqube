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
package org.sonar.server.ui;

import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;
import org.sonar.api.web.page.PageDefinition;

/**
 * @deprecated since 6.3 see {@link org.sonar.api.web.page.PageDefinition}. This class logs warnings at startup if there are pages using the old page API.
 */
@ServerSide
@Deprecated
public class DeprecatedViews implements Startable {

  private static final Logger LOGGER = Loggers.get(DeprecatedViews.class);

  private final View[] views;

  public DeprecatedViews() {
    this.views = new View[0];
  }

  public DeprecatedViews(View[] views) {
    this.views = views;
  }

  @Override
  public void start() {
    for (View view : views) {
      String pageOrWidget = view instanceof Widget ? "Widget" : "Page";
      LOGGER.warn("{} '{}' ({}) is ignored. See {} to define pages.", pageOrWidget, view.getTitle(), view.getId(), PageDefinition.class.getCanonicalName());
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
