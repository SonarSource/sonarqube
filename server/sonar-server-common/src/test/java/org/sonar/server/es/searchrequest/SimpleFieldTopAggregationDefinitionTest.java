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
package org.sonar.server.es.searchrequest;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SimpleFieldTopAggregationDefinitionTest {
  private static final Random RANDOM = new Random();


  @Test
  public void fieldName_cannot_be_null() {
    boolean sticky = RANDOM.nextBoolean();

    assertThatThrownBy(() -> new SimpleFieldTopAggregationDefinition(null, sticky))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("fieldName can't be null");
  }

  @Test
  public void getters() {
    String fieldName = RandomStringUtils.randomAlphabetic(12);
    boolean sticky = new Random().nextBoolean();
    SimpleFieldTopAggregationDefinition underTest = new SimpleFieldTopAggregationDefinition(fieldName, sticky);

    assertThat(underTest.getFilterScope().getFieldName()).isEqualTo(fieldName);
    assertThat(underTest.isSticky()).isEqualTo(sticky);
  }

  @Test
  public void getFilterScope_always_returns_the_same_instance() {
    String fieldName = randomAlphabetic(12);
    boolean sticky = RANDOM.nextBoolean();
    SimpleFieldTopAggregationDefinition underTest = new SimpleFieldTopAggregationDefinition(fieldName, sticky);

    Set<TopAggregationDefinition.FilterScope> filterScopes = IntStream.range(0, 2 + RANDOM.nextInt(200))
      .mapToObj(i -> underTest.getFilterScope())
      .collect(Collectors.toSet());

    assertThat(filterScopes).hasSize(1);
  }
}
