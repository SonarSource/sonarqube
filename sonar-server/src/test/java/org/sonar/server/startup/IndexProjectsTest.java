/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.persistence.resource.ResourceIndexerDao;
import org.sonar.persistence.resource.ResourceIndexerFilter;

import static org.mockito.Mockito.*;

public class IndexProjectsTest {

  @Test
  public void doNotIndexOnFreshInstalls() {
    ResourceIndexerDao indexerDao = mock(ResourceIndexerDao.class);
    ServerUpgradeStatus status = mock(ServerUpgradeStatus.class);
    when(status.isUpgraded()).thenReturn(false);
    when(status.isFreshInstall()).thenReturn(true);

    new IndexProjects(status, indexerDao).start();

    verifyZeroInteractions(indexerDao);
  }

  @Test
  public void doNotIndexOnUpgradesSince213() {
    ResourceIndexerDao indexerDao = mock(ResourceIndexerDao.class);
    ServerUpgradeStatus status = mock(ServerUpgradeStatus.class);
    when(status.isUpgraded()).thenReturn(true);
    when(status.isFreshInstall()).thenReturn(false);
    when(status.getInitialDbVersion()).thenReturn(SchemaMigration.VERSION_2_13 + 10);

    new IndexProjects(status, indexerDao).start();

    verifyZeroInteractions(indexerDao);
  }

  @Test
  public void doIndexOnUpgradeBefore213() {
    ResourceIndexerDao indexerDao = mock(ResourceIndexerDao.class);
    ServerUpgradeStatus status = mock(ServerUpgradeStatus.class);
    when(status.isUpgraded()).thenReturn(true);
    when(status.isFreshInstall()).thenReturn(false);
    when(status.getInitialDbVersion()).thenReturn(SchemaMigration.VERSION_2_13 - 10);

    new IndexProjects(status, indexerDao).start();

    verify(indexerDao).index(any(ResourceIndexerFilter.class));
  }
}
