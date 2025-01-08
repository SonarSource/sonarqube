/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.core.config.CorePropertyDefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.CoreProperties.DEVELOPMENT_COST;

public class RatingSettingsTest {

  private MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, CorePropertyDefinitions.all()));

  @Test
  public void load_rating_grid() {
    settings.setProperty(CoreProperties.RATING_GRID, "1,3.4,8,50");
    RatingSettings configurationLoader = new RatingSettings(settings.asConfig());

    double[] grid = configurationLoader.getDebtRatingGrid().getGridValues();
    assertThat(grid).hasSize(4);
    assertThat(grid[0]).isEqualTo(1.0);
    assertThat(grid[1]).isEqualTo(3.4);
    assertThat(grid[2]).isEqualTo(8.0);
    assertThat(grid[3]).isEqualTo(50.0);
  }

  @Test
  public void load_dev_cost() {
    settings.setProperty(DEVELOPMENT_COST, "50");
    RatingSettings configurationLoader = new RatingSettings(settings.asConfig());

    assertThat(configurationLoader.getDevCost()).isEqualTo(50L);
  }

  @Test
  public void fail_on_invalid_rating_grid_configuration() {
    assertThatThrownBy(() -> {
      settings.setProperty(CoreProperties.RATING_GRID, "a b c");
      new RatingSettings(settings.asConfig());
    })
      .isInstanceOf(IllegalArgumentException.class);

  }

}
