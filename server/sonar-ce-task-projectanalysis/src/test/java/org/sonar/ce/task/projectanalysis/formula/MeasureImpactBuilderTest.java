/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.formula;

import com.google.gson.Gson;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;


public class MeasureImpactBuilderTest {

  Gson gson = new Gson();

  @Test
  public void add_shouldAddExpectedInitialValues() {
    MeasureImpactBuilder measureImpactBuilder = new MeasureImpactBuilder();
    Assertions.assertThat(measureImpactBuilder.build()).isEqualTo("{}");

    measureImpactBuilder = new MeasureImpactBuilder();
    Map<String, Integer> map = Map.of("total", 3, Severity.HIGH.name(), 2, Severity.MEDIUM.name(), 1, Severity.LOW.name(), 4);
    measureImpactBuilder.add(gson.toJson(map));
    Assertions.assertThat(measureImpactBuilder.build()).isEqualTo(gson.toJson(map));
  }

  @Test
  public void add_shouldMergeValuesFromDifferentMaps() {

    MeasureImpactBuilder measureImpactBuilder = new MeasureImpactBuilder();
    Map<String, Integer> map = Map.of("total", 3, Severity.HIGH.name(), 2, Severity.MEDIUM.name(), 1, Severity.LOW.name(), 4);
    measureImpactBuilder.add(gson.toJson(map));

    Map<String, Integer> map2 = Map.of("total", 6, Severity.HIGH.name(), 4, Severity.MEDIUM.name(), 2, Severity.LOW.name(), 1);
    measureImpactBuilder.add(gson.toJson(map2));

    Assertions.assertThat(measureImpactBuilder.build()).isEqualTo(gson.toJson(Map.of("total", 9, Severity.HIGH.name(), 6, Severity.MEDIUM.name(), 3, Severity.LOW.name(), 5)));
  }

}
