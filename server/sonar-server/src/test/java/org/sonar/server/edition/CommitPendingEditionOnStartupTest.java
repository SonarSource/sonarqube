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

import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.license.LicenseCommit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_READY;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.MANUAL_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;

public class CommitPendingEditionOnStartupTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester()
      .setLevel(LoggerLevel.DEBUG);

  private MutableEditionManagementState editionManagementState = mock(MutableEditionManagementState.class);
  private LicenseCommit licenseCommit = mock(LicenseCommit.class);
  private CommitPendingEditionOnStartup underTest = new CommitPendingEditionOnStartup(editionManagementState);
  private CommitPendingEditionOnStartup underTestWithLicenseCommit = new CommitPendingEditionOnStartup(editionManagementState, licenseCommit);

  @Test
  public void start_has_no_effect_when_status_is_NONE_without_LicenseCommit() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(NONE);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verifyNoMoreInteractions(editionManagementState);
  }

  @Test
  public void start_has_no_effect_when_status_is_NONE_with_LicenseCommit() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(NONE);

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
  }

  @Test
  public void starts_has_no_effect_when_status_is_AUTOMATIC_READY_and_no_LicenseCommit_is_available_but_logs_at_debug_level() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_READY);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
        .containsOnly("No LicenseCommit instance is not available, can not finalize installation");
  }

  @Test
  public void start_commit_license_and_finalizeInstallation_in_editionManagementState_when_status_is_AUTOMATIC_READY_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_READY);
    String license = RandomStringUtils.randomAlphanumeric(20);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.of(license));

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).getPendingLicense();
    verify(editionManagementState).finalizeInstallation();
    verifyNoMoreInteractions(editionManagementState);
    verify(licenseCommit).update(license);
    verifyNoMoreInteractions(licenseCommit);
  }

  @Test
  public void starts_has_no_effect_when_status_is_MANUAL_IN_PROGRESS_and_no_LicenseCommit_is_available_but_logs_at_debug_level() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(MANUAL_IN_PROGRESS);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
        .containsOnly("No LicenseCommit instance is not available, can not finalize installation");
  }

  @Test
  public void start_commit_license_and_finalizeInstallation_in_editionManagementState_when_status_is_MANUAL_IN_PROGRESS_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(MANUAL_IN_PROGRESS);
    String license = RandomStringUtils.randomAlphanumeric(20);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.of(license));

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).getPendingLicense();
    verify(editionManagementState).finalizeInstallation();
    verifyNoMoreInteractions(editionManagementState);
    verify(licenseCommit).update(license);
    verifyNoMoreInteractions(licenseCommit);
  }

  @Test
  public void starts_put_editionManagement_set_in_automaticInstallError_when_status_is_AUTOMATIC_PROGRESS_and_no_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_IN_PROGRESS);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).installFailed("SonarQube was restarted before asynchronous installation of edition completed");
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
  }

  @Test
  public void starts_put_editionManagement_set_in_automaticInstallError_when_status_is_AUTOMATIC_PROGRESS_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_IN_PROGRESS);

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).installFailed("SonarQube was restarted before asynchronous installation of edition completed");
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
  }

  @Test
  public void should_commit_uninstall() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).finalizeInstallation();
    verifyNoMoreInteractions(editionManagementState);
  }

  @Test
  public void should_fail_uninstall_if_license_commit_is_present() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("License Manager plugin is still present");

    underTestWithLicenseCommit.start();
  }
}
