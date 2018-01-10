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
package org.sonar.db.qualitygate;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.measures.Metric.ValueType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.db.qualitygate.QualityGateConditionDto.isOperatorAllowed;

public class QualityGateConditionDtoTest {

  @Test
  public void validate_operators_for_DATA() {
    assertThat(isOperatorAllowed("WHATEVER", DATA)).isFalse();
  }

  @Test
  public void validate_operators_for_BOOL() {
    assertThat(isOperatorAllowed("EQ", BOOL)).isTrue();
    assertThat(isOperatorAllowed("NE", BOOL)).isFalse();
    assertThat(isOperatorAllowed("LT", BOOL)).isFalse();
    assertThat(isOperatorAllowed("GT", BOOL)).isFalse();
  }

  @Test
  public void validate_operators_for_LEVEL() {
    assertThat(isOperatorAllowed("EQ", LEVEL)).isTrue();
    assertThat(isOperatorAllowed("NE", LEVEL)).isTrue();
    assertThat(isOperatorAllowed("LT", LEVEL)).isFalse();
    assertThat(isOperatorAllowed("GT", LEVEL)).isFalse();
  }

  @Test
  public void validate_operators_for_RATING() {
    assertThat(isOperatorAllowed("EQ", RATING)).isFalse();
    assertThat(isOperatorAllowed("NE", RATING)).isFalse();
    assertThat(isOperatorAllowed("LT", RATING)).isFalse();
    assertThat(isOperatorAllowed("GT", RATING)).isTrue();
  }

  @Test
  public void validate_operators_for_other_types() {
    Arrays.stream(new ValueType[] {STRING, INT, FLOAT, PERCENT, MILLISEC}).forEach(type -> {
      assertThat(isOperatorAllowed("EQ", type)).isTrue();
      assertThat(isOperatorAllowed("NE", type)).isTrue();
      assertThat(isOperatorAllowed("LT", type)).isTrue();
      assertThat(isOperatorAllowed("GT", type)).isTrue();
    });
  }

}
