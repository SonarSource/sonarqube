/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.computation.taskprocessor;

import org.junit.Test;
import org.sonar.ce.log.CeLogging;
import org.sonar.server.computation.queue.InternalCeQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CeWorkerFactoryImplTest {
  @Test
  public void ceworker_created_by_factory_must_contain_uuid() {
    CeWorkerFactoryImpl underTest = new CeWorkerFactoryImpl(mock(InternalCeQueue.class), mock(CeLogging.class), mock(CeTaskProcessorRepository.class));
    CeWorker ceWorker = underTest.create();
    assertThat(ceWorker).isInstanceOf(CeWorkerImpl.class);
    assertThat(((CeWorkerImpl) ceWorker).getUuid()).isNotEmpty();
  }
}
