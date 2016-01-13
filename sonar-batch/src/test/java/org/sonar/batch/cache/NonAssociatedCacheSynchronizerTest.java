/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.cache;

import com.google.common.collect.ImmutableList;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.rule.LoadedActiveRule;
import org.sonar.batch.rule.RulesLoader;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class NonAssociatedCacheSynchronizerTest {
  private NonAssociatedCacheSynchronizer synchronizer;

  @Mock
  private RulesLoader rulesLoader;
  @Mock
  private QualityProfileLoader qualityProfileLoader;
  @Mock
  private ActiveRulesLoader activeRulesLoader;
  @Mock
  private ProjectCacheStatus cacheStatus;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    QualityProfile pf = QualityProfile.newBuilder().setKey("profile").setName("profile").setLanguage("lang").build();
    LoadedActiveRule ar = new LoadedActiveRule();

    when(qualityProfileLoader.loadDefault(null, null)).thenReturn(ImmutableList.of(pf));
    when(activeRulesLoader.load("profile", null)).thenReturn(ImmutableList.of(ar));

    synchronizer = new NonAssociatedCacheSynchronizer(rulesLoader, qualityProfileLoader, activeRulesLoader, cacheStatus);
  }

  @Test
  public void dont_sync_if_exists() {
    when(cacheStatus.getSyncStatus()).thenReturn(new Date());
    synchronizer.execute(false);
    verifyZeroInteractions(rulesLoader, qualityProfileLoader, activeRulesLoader);
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
    verify(rulesLoader).load(null);
    verify(qualityProfileLoader).loadDefault(null, null);
    verify(activeRulesLoader).load("profile", null);

    verifyNoMoreInteractions(qualityProfileLoader, activeRulesLoader);
  }
}
