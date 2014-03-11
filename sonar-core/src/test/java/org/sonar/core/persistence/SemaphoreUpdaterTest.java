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
package org.sonar.core.persistence;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.Semaphores;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SemaphoreUpdaterTest extends AbstractDaoTestCase {

  private SemaphoreUpdater updater;
  private SemaphoreDao dao;

  @Before
  public void before() {
    dao = mock(SemaphoreDao.class);
    updater = new SemaphoreUpdater(dao);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testScheduleUpdate() throws Exception {
    Semaphores.Semaphore semaphore = new Semaphores.Semaphore().setName("foo");
    updater.scheduleForUpdate(semaphore, 1);

    Thread.sleep(2000);

    verify(dao, atLeastOnce()).update(semaphore);
  }

  @Test
  public void testCancelUpdate() throws Exception {
    Semaphores.Semaphore semaphore = new Semaphores.Semaphore().setName("foo");
    updater.scheduleForUpdate(semaphore, 1);
    updater.stopUpdate("foo");

    Thread.sleep(2000);

    verify(dao, never()).update(semaphore);
  }

  @Test
  public void shouldNotFailWhenCancelNotExistingSemaphore() throws Exception {
    updater.stopUpdate("foo");
  }

}
