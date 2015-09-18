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
package org.sonar.batch.cache;

import static org.mockito.Mockito.when;
import org.sonar.batch.protocol.input.ActiveRule;
import com.google.common.collect.ImmutableList;
import org.sonar.batch.protocol.input.QProfile;
import org.junit.Test;

import java.util.Date;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.mockito.MockitoAnnotations;
import org.junit.Before;
import org.mockito.Mock;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.repository.QualityProfileLoader;

public class NonAssociatedCacheSynchronizerTest {
  private NonAssociatedCacheSynchronizer synchronizer;

  @Mock
  private QualityProfileLoader qualityProfileLoader;
  @Mock
  private ActiveRulesLoader activeRulesLoader;
  @Mock
  private ProjectCacheStatus cacheStatus;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    QProfile pf = new QProfile("profile", "profile", "lang", new Date(1000));
    ActiveRule ar = mock(ActiveRule.class);

    when(qualityProfileLoader.load(null, null)).thenReturn(ImmutableList.of(pf));
    when(activeRulesLoader.load(ImmutableList.of("profile"), null)).thenReturn(ImmutableList.of(ar));

    synchronizer = new NonAssociatedCacheSynchronizer(qualityProfileLoader, activeRulesLoader, cacheStatus);
  }

  @Test
  public void dont_sync_if_exists() {
    when(cacheStatus.getSyncStatus()).thenReturn(new Date());
    synchronizer.execute(false);
    verifyNoMoreInteractions(qualityProfileLoader, activeRulesLoader);
  }

  @Test
  public void always_sync_if_force() {
    when(cacheStatus.getSyncStatus()).thenReturn(new Date());
    synchronizer.execute(true);
    checkSync();
  }

  @Test
  public void sync_if_doesnt_exist() {
    synchronizer.execute(false);
    checkSync();
  }

  private void checkSync() {
    verify(cacheStatus).getSyncStatus();
    verify(cacheStatus).save();
    verify(qualityProfileLoader).load(null, null);
    verify(activeRulesLoader).load(ImmutableList.of("profile"), null);

    verifyNoMoreInteractions(qualityProfileLoader, activeRulesLoader);
  }
}
