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
package org.sonar.api.batch;

import org.junit.Test;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;

public class DefaultFormulaDataTest {

  DefaultFormulaData underTest = new DefaultFormulaData(null);

  @Test(expected = UnsupportedOperationException.class)
  public void fail_if_used_1() {
    underTest.getChildren();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fail_if_used_2() {
    underTest.getChildrenMeasures((MeasuresFilter) null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fail_if_used_3() {
    underTest.getChildrenMeasures((Metric) null);
  }
}
