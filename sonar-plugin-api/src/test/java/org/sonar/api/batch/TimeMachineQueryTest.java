/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import org.sonar.api.measures.CoreMetrics;

import java.util.Arrays;

public class TimeMachineQueryTest {

  @Test
  public void setNullMetrics() {
    TimeMachineQuery query = new TimeMachineQuery(null).setMetrics(Arrays.asList(CoreMetrics.LINES));
    assertThat(query.getMetrics(), hasItem(CoreMetrics.LINES));

    query.unsetMetrics();
    assertThat(query.getMetrics(), nullValue());

    query.setMetrics(CoreMetrics.LINES, CoreMetrics.COVERAGE);
    assertThat(query.getMetrics(), hasItems(CoreMetrics.LINES, CoreMetrics.COVERAGE));
  }
}
