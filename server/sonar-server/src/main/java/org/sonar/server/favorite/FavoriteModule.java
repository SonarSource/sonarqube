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
package org.sonar.server.favorite;

import org.sonar.core.platform.Module;
import org.sonar.server.favorite.ws.AddAction;
import org.sonar.server.favorite.ws.FavoritesWs;
import org.sonar.server.favorite.ws.FavouritesWs;
import org.sonar.server.favorite.ws.RemoveAction;
import org.sonar.server.favorite.ws.SearchAction;

public class FavoriteModule extends Module {

  @Override
  protected void configureModule() {
    add(
      FavouritesWs.class,
      FavoriteFinder.class,
      FavoriteUpdater.class,
      FavoritesWs.class,
      AddAction.class,
      RemoveAction.class,
      SearchAction.class);
  }

}
