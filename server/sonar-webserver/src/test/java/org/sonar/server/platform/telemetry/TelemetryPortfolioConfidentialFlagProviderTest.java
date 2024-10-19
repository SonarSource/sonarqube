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
package org.sonar.server.platform.telemetry;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.telemetry.core.Dimension.INSTALLATION;
import static org.sonar.telemetry.core.Granularity.WEEKLY;
import static org.sonar.telemetry.core.TelemetryDataType.BOOLEAN;

class TelemetryPortfolioConfidentialFlagProviderTest {

  private final DbClient dbClient = Mockito.mock();
  private final PropertiesDao propertiesDao = mock();
  private final TelemetryPortfolioConfidentialFlagProvider underTest = new TelemetryPortfolioConfidentialFlagProvider(dbClient);

  @ParameterizedTest
  @MethodSource("getValues")
  void getter_should_return_correct_values(Boolean value, Boolean expected) {
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    if (value == null) {
      when(dbClient.propertiesDao().selectGlobalProperty("sonar.portfolios.confidential.header"))
        .thenReturn(null);
    } else {
      when(dbClient.propertiesDao().selectGlobalProperty("sonar.portfolios.confidential.header"))
        .thenReturn(new PropertyDto().setValue(value.toString()));
    }

    assertEquals("portfolio_reports_confidential_flag", underTest.getMetricKey());
    assertEquals(INSTALLATION, underTest.getDimension());
    assertEquals(WEEKLY, underTest.getGranularity());
    assertEquals(BOOLEAN, underTest.getType());
    assertEquals(Optional.of(expected), underTest.getValue());
  }

  public static Stream<Arguments> getValues() {
    return Stream.of(
      Arguments.of(true, true),
      Arguments.of(false, false),
      Arguments.of(null, true)
    );
  }
}
