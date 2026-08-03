/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.history.controller;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectBreakdownRequestValidatorTest {

  private static final String PORTFOLIO_ID = "portfolio-uuid";
  private static final String ENTITY_ID = "entity-uuid";

  @Test
  void validateSelectorAcceptsPortfolioId() {
    assertThatCode(() -> ProjectBreakdownRequestValidator.validateSelector(PORTFOLIO_ID, null, null))
      .doesNotThrowAnyException();
  }

  @Test
  void validateSelectorAcceptsEntityTypeAndId() {
    assertThatCode(() -> ProjectBreakdownRequestValidator.validateSelector(null, "PORTFOLIO", ENTITY_ID))
      .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("invalidSelectors")
  void validateSelectorRejectsInvalidCombinations(
    @Nullable String portfolioId,
    @Nullable String entityType,
    @Nullable String entityId,
    String expectedMessage) {
    assertThatThrownBy(() -> ProjectBreakdownRequestValidator.validateSelector(portfolioId, entityType, entityId))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedMessage);
  }

  private static Stream<Arguments> invalidSelectors() {
    String missingSelectorMessage = "Either portfolioId or both entityType and entityId must be provided";
    String mixedSelectorMessage = "portfolioId cannot be combined with entityType or entityId";
    return Stream.of(
      Arguments.of(null, null, null, missingSelectorMessage),
      Arguments.of(null, "APPLICATION", null, missingSelectorMessage),
      Arguments.of(null, null, ENTITY_ID, missingSelectorMessage),
      Arguments.of(PORTFOLIO_ID, "PORTFOLIO", ENTITY_ID, mixedSelectorMessage),
      Arguments.of(PORTFOLIO_ID, "PORTFOLIO", null, mixedSelectorMessage),
      Arguments.of(PORTFOLIO_ID, null, ENTITY_ID, mixedSelectorMessage));
  }
}
