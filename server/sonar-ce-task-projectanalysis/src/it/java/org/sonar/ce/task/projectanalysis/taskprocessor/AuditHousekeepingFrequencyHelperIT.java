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
import static org.sonar.core.config.PurgeConstants.AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeConstants.DEFAULT_AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeConstants.MAX_AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeConstants.MIN_AUDIT_PURGE_BATCH_SIZE;
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

  @Test
  public void getPurgeBatchSize_returnsConfiguredValue() {
    PropertyDto propertyDto = new PropertyDto().setKey(AUDIT_PURGE_BATCH_SIZE).setValue("500000");
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao.selectGlobalProperty(dbSession, AUDIT_PURGE_BATCH_SIZE)).thenReturn(propertyDto);

    assertThat(underTest.getPurgeBatchSize(dbClient, dbSession)).isEqualTo(500000);
  }

  @Test
  public void getPurgeBatchSize_returnsDefaultWhenNotSet() {
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao.selectGlobalProperty(dbSession, AUDIT_PURGE_BATCH_SIZE)).thenReturn(null);

    assertThat(underTest.getPurgeBatchSize(dbClient, dbSession)).isEqualTo(DEFAULT_AUDIT_PURGE_BATCH_SIZE);
  }

  @Test
  public void getPurgeBatchSize_returnsDefaultWhenValueIsNotNumeric() {
    PropertyDto propertyDto = new PropertyDto().setKey(AUDIT_PURGE_BATCH_SIZE).setValue("not-a-number");
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao.selectGlobalProperty(dbSession, AUDIT_PURGE_BATCH_SIZE)).thenReturn(propertyDto);

    assertThat(underTest.getPurgeBatchSize(dbClient, dbSession)).isEqualTo(DEFAULT_AUDIT_PURGE_BATCH_SIZE);
  }

  @Test
  @UseDataProvider("outOfRangeValues")
  public void getPurgeBatchSize_returnsDefaultWhenValueIsOutOfRange(String value) {
    PropertyDto propertyDto = new PropertyDto().setKey(AUDIT_PURGE_BATCH_SIZE).setValue(value);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao.selectGlobalProperty(dbSession, AUDIT_PURGE_BATCH_SIZE)).thenReturn(propertyDto);

    assertThat(underTest.getPurgeBatchSize(dbClient, dbSession)).isEqualTo(DEFAULT_AUDIT_PURGE_BATCH_SIZE);
  }

  @Test
  public void getPurgeBatchSize_returnsValueWhenAtMax() {
    PropertyDto propertyDto = new PropertyDto().setKey(AUDIT_PURGE_BATCH_SIZE).setValue(String.valueOf(MAX_AUDIT_PURGE_BATCH_SIZE));
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(propertiesDao.selectGlobalProperty(dbSession, AUDIT_PURGE_BATCH_SIZE)).thenReturn(propertyDto);

    assertThat(underTest.getPurgeBatchSize(dbClient, dbSession)).isEqualTo(MAX_AUDIT_PURGE_BATCH_SIZE);
  }

  @DataProvider
  public static Object[][] outOfRangeValues() {
    return new Object[][] {
      {String.valueOf(MIN_AUDIT_PURGE_BATCH_SIZE - 1)},
      {String.valueOf(MAX_AUDIT_PURGE_BATCH_SIZE + 1)}
    };
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
