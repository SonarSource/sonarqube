/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.es.searchrequest;

import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class TopAggregationDefTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fieldName_cannot_be_null() {
    boolean sticky = new Random().nextBoolean();
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("fieldName can't be null");

    new TopAggregationDef(null, sticky);
  }

  @Test
  public void getters() {
    String fieldName = RandomStringUtils.randomAlphabetic(12);
    boolean sticky = new Random().nextBoolean();
    TopAggregationDef underTest = new TopAggregationDef(fieldName, sticky);

    assertThat(underTest.getFieldName()).isEqualTo(fieldName);
    assertThat(underTest.isSticky()).isEqualTo(sticky);
  }
}
