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
package org.sonar.server.edition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.edition.EditionManagementState.PendingStatus;
import org.sonar.server.license.LicenseCommit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CommitPendingEditionOnStartupTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private MutableEditionManagementState mutableEditionManagementState = mock(MutableEditionManagementState.class);

  @Test
  public void should_commit_uninstall() {
    CommitPendingEditionOnStartup underTest = new CommitPendingEditionOnStartup(mutableEditionManagementState);
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(PendingStatus.UNINSTALL_IN_PROGRESS);

    underTest.start();

    verify(mutableEditionManagementState).getPendingInstallationStatus();
    verify(mutableEditionManagementState).finalizeInstallation();
    verifyNoMoreInteractions(mutableEditionManagementState);
  }

  @Test
  public void should_fail_uninstall_if_license_commit_is_present() {
    when(mutableEditionManagementState.getPendingInstallationStatus()).thenReturn(PendingStatus.UNINSTALL_IN_PROGRESS);
    CommitPendingEditionOnStartup underTest = new CommitPendingEditionOnStartup(mutableEditionManagementState,
      mock(LicenseCommit.class));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("License Manager plugin is still present");
    underTest.start();
  }

}
