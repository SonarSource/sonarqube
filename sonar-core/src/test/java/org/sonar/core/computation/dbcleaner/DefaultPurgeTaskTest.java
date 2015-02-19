/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.computation.dbcleaner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.log.Logger;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeListener;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class DefaultPurgeTaskTest {

  private DefaultPurgeTask sut;
  private ResourceDao resourceDao;
  private PurgeDao purgeDao;
  private Settings settings;
  private DefaultPeriodCleaner periodCleaner;
  private PurgeProfiler profiler;

  @Before
  public void before() throws Exception {
    this.purgeDao = mock(PurgeDao.class);
    this.resourceDao = mock(ResourceDao.class);
    when(resourceDao.getResource(anyLong())).thenReturn(new ResourceDto().setQualifier(Qualifiers.PROJECT).setUuid("1").setId(1L));

    this.settings = mock(Settings.class);
    this.periodCleaner = mock(DefaultPeriodCleaner.class);
    this.profiler = mock(PurgeProfiler.class);

    this.sut = new DefaultPurgeTask(purgeDao, resourceDao, settings, periodCleaner, profiler);
  }

  @Test
  public void shouldNotDeleteHistoricalDataOfDirectories() {
    PurgeDao purgeDao = mock(PurgeDao.class);
    Settings settings = new Settings(new PropertyDefinitions(DataCleanerProperties.all()));
    settings.setProperty(DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY, "false");
    DefaultPurgeTask task = new DefaultPurgeTask(purgeDao, resourceDao, settings, mock(DefaultPeriodCleaner.class), mock(PurgeProfiler.class));

    task.purge(1L);

    verify(purgeDao).purge(argThat(new ArgumentMatcher<PurgeConfiguration>() {
      @Override
      public boolean matches(Object o) {
        PurgeConfiguration conf = (PurgeConfiguration) o;
        return conf.rootProjectIdUuid().getId() == 1L && conf.scopesWithoutHistoricalData().length == 1 && conf.scopesWithoutHistoricalData()[0].equals(Scopes.FILE);
      }
    }), any(PurgeListener.class));
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesByDefault() {
    PurgeDao purgeDao = mock(PurgeDao.class);
    Settings settings = new Settings(new PropertyDefinitions(DataCleanerProperties.all()));
    DefaultPurgeTask task = new DefaultPurgeTask(purgeDao, resourceDao, settings, mock(DefaultPeriodCleaner.class), mock(PurgeProfiler.class));

    task.purge(1L);

    verify(purgeDao).purge(argThat(new ArgumentMatcher<PurgeConfiguration>() {
      @Override
      public boolean matches(Object o) {
        PurgeConfiguration conf = (PurgeConfiguration) o;
        return conf.rootProjectIdUuid().getId() == 1L &&
          conf.scopesWithoutHistoricalData().length == 2 &&
          conf.scopesWithoutHistoricalData()[0].equals(Scopes.DIRECTORY) &&
          conf.scopesWithoutHistoricalData()[1].equals(Scopes.FILE);
      }
    }), any(PurgeListener.class));
  }

  @Test
  public void shouldNotFailOnErrors() {
    PurgeDao purgeDao = mock(PurgeDao.class);
    when(purgeDao.purge(any(PurgeConfiguration.class), any(PurgeListener.class))).thenThrow(new RuntimeException());
    DefaultPurgeTask task = new DefaultPurgeTask(purgeDao, resourceDao, new Settings(), mock(DefaultPeriodCleaner.class), mock(PurgeProfiler.class));

    task.purge(1L);

    verify(purgeDao, times(1)).purge(any(PurgeConfiguration.class), any(PurgeListener.class));
  }

  @Test
  public void shouldDumpProfiling() {
    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30);
    PurgeDao purgeDao = mock(PurgeDao.class);
    when(purgeDao.purge(conf, PurgeListener.EMPTY)).thenThrow(new RuntimeException());
    Settings settings = new Settings(new PropertyDefinitions(DataCleanerProperties.all()));
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, true);
    PurgeProfiler profiler = mock(PurgeProfiler.class);

    DefaultPurgeTask task = new DefaultPurgeTask(purgeDao, resourceDao, settings, mock(DefaultPeriodCleaner.class), profiler);
    task.purge(1L);

    verify(profiler).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void recognize_view_and_subview() {
    boolean viewCheck = sut.isNotViewNorSubview(Qualifiers.VIEW);
    boolean subViewCheck = sut.isNotViewNorSubview(Qualifiers.SUBVIEW);

    assertThat(viewCheck).isFalse();
    assertThat(subViewCheck).isFalse();
  }

  @Test
  public void call_dao_delete_when_deleting() throws Exception {
    when(resourceDao.getResource(123L)).thenReturn(new ResourceDto().setId(123L).setUuid("A"));

    sut.delete(123L);

    verify(purgeDao, times(1)).deleteResourceTree(any(IdUuidPair.class));
  }
}
