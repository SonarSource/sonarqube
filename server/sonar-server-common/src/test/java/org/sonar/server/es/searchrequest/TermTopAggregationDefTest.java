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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class TermTopAggregationDefTest {
  private static final Random RANDOM = new Random();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fieldName_cannot_be_null() {
    boolean sticky = RANDOM.nextBoolean();
    int maxTerms = 10;

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("fieldName can't be null");

    new TermTopAggregationDef(null, sticky, maxTerms);
  }

  @Test
  public void maxTerms_can_be_null() {
    String fieldName = randomAlphabetic(12);
    boolean sticky = RANDOM.nextBoolean();

    TermTopAggregationDef underTest = new TermTopAggregationDef(fieldName, sticky, null);
    assertThat(underTest.getMaxTerms()).isEmpty();
  }

  @Test
  public void maxTerms_can_be_0() {
    String fieldName = randomAlphabetic(12);
    boolean sticky = RANDOM.nextBoolean();

    TermTopAggregationDef underTest = new TermTopAggregationDef(fieldName, sticky, 0);
    assertThat(underTest.getMaxTerms()).hasValue(0);
  }

  @Test
  public void maxTerms_cant_be_less_than_0() {
    String fieldName = randomAlphabetic(12);
    boolean sticky = RANDOM.nextBoolean();
    int negativeNumber = -1 - RANDOM.nextInt(200);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("maxTerms can't be < 0");

    new TermTopAggregationDef(fieldName, sticky, negativeNumber);
  }

  @Test
  public void getters() {
    String fieldName = randomAlphabetic(12);
    boolean sticky = RANDOM.nextBoolean();
    int maxTerms = RANDOM.nextInt(299);
    TermTopAggregationDef underTest = new TermTopAggregationDef(fieldName, sticky, maxTerms);

    assertThat(underTest.getFieldName()).isEqualTo(fieldName);
    assertThat(underTest.isSticky()).isEqualTo(sticky);
    assertThat(underTest.getMaxTerms()).hasValue(maxTerms);
  }

}
