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
package org.sonarqube.ws.tester;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public class QModelTester {

  private static final String DEV_COST_PROPERTY = "sonar.technicalDebt.developmentCost";
  private static final String RATING_GRID_PROPERTY = "sonar.technicalDebt.ratingGrid";
  private static final Joiner COMMA_JOINER = Joiner.on(",");

  private final TesterSession session;

  QModelTester(TesterSession session) {
    this.session = session;
  }

  public void updateDevelopmentCost(int developmentCost) {
    session.settings().setGlobalSettings(DEV_COST_PROPERTY, Integer.toString(developmentCost));
  }

  public void updateRatingGrid(Double... ratingGrid) {
    Preconditions.checkState(ratingGrid.length == 4, "Rating grid must contains 4 values");
    session.settings().setGlobalSettings(RATING_GRID_PROPERTY, COMMA_JOINER.join(ratingGrid));
  }

  public void reset() {
    session.settings().resetSettings(RATING_GRID_PROPERTY, DEV_COST_PROPERTY);
  }
}
