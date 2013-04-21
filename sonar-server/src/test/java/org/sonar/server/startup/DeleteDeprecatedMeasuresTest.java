/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteDeprecatedMeasuresTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldDeleteMeasuresWithCategory() {
    setupData("sharedFixture");

    ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);

    final DeleteDeprecatedMeasures purge = new DeleteDeprecatedMeasures(getSessionFactory(), upgradeStatus);
    purge.doPurge();

    List rows = getSession().createQuery("from " + MeasureModel.class.getSimpleName() + " where ruleId is null and rulesCategoryId is not null").getResultList();
    assertThat(rows.size(), is(0));
  }

  @Test
  public void shouldDeleteMeasuresWithPriority() {
    setupData("sharedFixture");

    ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
    final DeleteDeprecatedMeasures purge = new DeleteDeprecatedMeasures(getSessionFactory(), upgradeStatus);
    purge.doPurge();

    List rowsToDelete = getSession().createQuery("from " + MeasureModel.class.getSimpleName() + " where ruleId is null and rulePriority is not null").getResultList();
    assertThat(rowsToDelete.size(), is(0));

    List<Long> rowIdsToKeep = getSession().createQuery("select id from " + MeasureModel.class.getSimpleName()).getResultList();
    assertThat(rowIdsToKeep, hasItems(1L, 2L, 4L, 6L));
  }

  @Test
  public void shouldDoPurgeOnUpgradeBefore162() {
    ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
    when(upgradeStatus.isUpgraded()).thenReturn(true);
    when(upgradeStatus.getInitialDbVersion()).thenReturn(50);

    final DeleteDeprecatedMeasures purge = new DeleteDeprecatedMeasures(getSessionFactory(), upgradeStatus);
    assertThat(purge.mustDoPurge(), is(true));
  }

  @Test
  public void shouldNotDoPurgeOnUpgradeAfter162() {
    ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
    when(upgradeStatus.isUpgraded()).thenReturn(true);
    when(upgradeStatus.getInitialDbVersion()).thenReturn(170);

    final DeleteDeprecatedMeasures purge = new DeleteDeprecatedMeasures(getSessionFactory(), upgradeStatus);
    assertThat(purge.mustDoPurge(), is(false));
  }

  @Test
  public void shouldNotDoPurgeOnFreshInstall() {
    ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
    when(upgradeStatus.isUpgraded()).thenReturn(false);
    when(upgradeStatus.getInitialDbVersion()).thenReturn(-1);

    final DeleteDeprecatedMeasures purge = new DeleteDeprecatedMeasures(getSessionFactory(), upgradeStatus);
    assertThat(purge.mustDoPurge(), is(false));
  }

  @Test
  public void shouldNotDoPurgeOnStandardStartup() {
    ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
    when(upgradeStatus.isUpgraded()).thenReturn(false);
    when(upgradeStatus.getInitialDbVersion()).thenReturn(DatabaseVersion.LAST_VERSION);

    final DeleteDeprecatedMeasures purge = new DeleteDeprecatedMeasures(getSessionFactory(), upgradeStatus);
    assertThat(purge.mustDoPurge(), is(false));
  }
}
