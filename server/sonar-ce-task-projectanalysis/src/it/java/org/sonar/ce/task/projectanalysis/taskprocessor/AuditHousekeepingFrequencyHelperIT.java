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
package org.sonar.ce.task.projectanalysis.taskprocessor;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.config.Frequency;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.PurgeConstants.AUDIT_HOUSEKEEPING_FREQUENCY;
import static org.sonar.core.config.PurgeProperties.DEFAULT_FREQUENCY;

@RunWith(DataProviderRunner.class)
public class AuditHousekeepingFrequencyHelperIT {
  private static final long NOW = 10_000_000_000L;

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final AuditHousekeepingFrequencyHelper underTest = new AuditHousekeepingFrequencyHelper(system2);

  @Test
  @UseDataProvider("frequencyOptions")
  public void getThresholdDate(Frequency frequency) {
    long result = underTest.getThresholdDate(frequency.getDescription());


    long expected = Instant.ofEpochMilli(system2.now())
      .minus(frequency.getDays(), ChronoUnit.DAYS)
      .toEpochMilli();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getThresholdDateForUnknownFrequencyFails() {
    assertThatThrownBy(() -> underTest.getThresholdDate("Lalala"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported frequency: Lalala");
  }

  @Test
  public void getHouseKeepingFrequency() {
    String value = "Weekly";
    PropertyDto propertyDto = new PropertyDto().setKey(AUDIT_HOUSEKEEPING_FREQUENCY).setValue(value);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao
      .selectGlobalProperty(dbSession, AUDIT_HOUSEKEEPING_FREQUENCY))
      .thenReturn(propertyDto);
    assertThat(underTest.getHouseKeepingFrequency(dbClient, dbSession).getValue()).isEqualTo(value);
  }

  @Test
  public void getDefaultHouseKeepingFrequencyWhenNotSet() {
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao
      .selectGlobalProperty(dbSession, AUDIT_HOUSEKEEPING_FREQUENCY))
      .thenReturn(null);
    assertThat(underTest.getHouseKeepingFrequency(dbClient, dbSession).getValue())
      .isEqualTo(DEFAULT_FREQUENCY);
  }

  @DataProvider
  public static Object[][] frequencyOptions() {
    return new Object[][] {
      {Frequency.WEEKLY},
      {Frequency.MONTHLY},
      {Frequency.TRIMESTRIAL},
      {Frequency.YEARLY}
    };
  }
}
