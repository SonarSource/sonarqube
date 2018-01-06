/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_READY;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.MANUAL_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS;

public class FinalizeEditionChangeTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  private MutableEditionManagementState editionManagementState = mock(MutableEditionManagementState.class);
  private LicenseCommit licenseCommit = mock(LicenseCommit.class);
  private FinalizeEditionChange underTest = new FinalizeEditionChange(editionManagementState);
  private FinalizeEditionChange underTestWithLicenseCommit = new FinalizeEditionChange(editionManagementState, licenseCommit);

  @Test
  public void start_clears_error_message_when_status_is_NONE_without_LicenseCommit() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(NONE);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).clearInstallErrorMessage();
    verifyNoMoreInteractions(editionManagementState);
  }

  @Test
  public void start_clears_error_message_when_status_is_NONE_with_LicenseCommit() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(NONE);

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).clearInstallErrorMessage();
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
  }

  @Test
  public void start_clears_status_when_status_is_AUTOMATIC_READY_and_no_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_READY);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).finalizeInstallation("Edition installation didn't complete. Some plugins were not installed.");

    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("Edition installation didn't complete. Some plugins were not installed.");
  }

  @Test
  public void start_clears_status_when_status_is_AUTOMATIC_READY_and_license_is_not_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_READY);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.empty());

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).finalizeInstallation("Edition installation didn't complete. License was not found.");
    verify(editionManagementState).getPendingLicense();

    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("Edition installation didn't complete. License was not found.");
  }

  @Test
  public void start_commit_license_and_finalizeInstallation_in_editionManagementState_when_status_is_AUTOMATIC_READY_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_READY);
    String license = RandomStringUtils.randomAlphanumeric(20);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.of(license));

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).getPendingLicense();
    verify(editionManagementState).finalizeInstallation(null);
    verifyNoMoreInteractions(editionManagementState);
    verify(licenseCommit).update(license);
    verifyNoMoreInteractions(licenseCommit);
  }

  @Test
  public void start_commit_license_and_finalizeInstallation_with_error_in_editionManagementState_when_status_is_AUTOMATIC_READY_and_license_is_invalid() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_READY);
    String license = RandomStringUtils.randomAlphanumeric(20);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.of(license));
    doThrow(new IllegalArgumentException("Faking an IAE because of an invalid license"))
      .when(licenseCommit)
      .update(license);

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).getPendingLicense();
    verify(editionManagementState).finalizeInstallation("Edition installation didn't complete. License is not valid. Please set a new license.");
    verifyNoMoreInteractions(editionManagementState);
    verify(licenseCommit).update(license);
    verifyNoMoreInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("Edition installation didn't complete. License is not valid. Please set a new license.");
  }

  @Test
  public void start_clears_status_when_status_is_MANUAL_IN_PROGRESS_and_no_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(MANUAL_IN_PROGRESS);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).finalizeInstallation("Edition installation didn't complete. Some plugins were not installed.");
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("Edition installation didn't complete. Some plugins were not installed.");
  }

  @Test
  public void start_commit_license_and_finalizeInstallation_in_editionManagementState_when_status_is_MANUAL_IN_PROGRESS_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(MANUAL_IN_PROGRESS);
    String license = RandomStringUtils.randomAlphanumeric(20);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.of(license));

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).getPendingLicense();
    verify(editionManagementState).finalizeInstallation(null);
    verifyNoMoreInteractions(editionManagementState);
    verify(licenseCommit).update(license);
    verifyNoMoreInteractions(licenseCommit);
  }

  @Test
  public void start_commit_license_and_finalizeInstallation_with_error_in_editionManagementState_when_status_is_MANUAL_IN_PROGRESS_and_license_is_invalid() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(MANUAL_IN_PROGRESS);
    String license = RandomStringUtils.randomAlphanumeric(20);
    when(editionManagementState.getPendingLicense()).thenReturn(Optional.of(license));
    doThrow(new IllegalArgumentException("Faking an IAE because of an invalid license"))
      .when(licenseCommit)
      .update(license);

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).getPendingLicense();
    verify(editionManagementState).finalizeInstallation("Edition installation didn't complete. License is not valid. Please set a new license.");
    verifyNoMoreInteractions(editionManagementState);
    verify(licenseCommit).update(license);
    verifyNoMoreInteractions(licenseCommit);
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("Edition installation didn't complete. License is not valid. Please set a new license.");
  }

  @Test
  public void start_put_editionManagement_set_in_automaticInstallError_when_status_is_AUTOMATIC_PROGRESS_and_no_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_IN_PROGRESS);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).installFailed("SonarQube was restarted before asynchronous installation of edition completed");
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
  }

  @Test
  public void start_put_editionManagement_set_in_automaticInstallError_when_status_is_AUTOMATIC_PROGRESS_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(AUTOMATIC_IN_PROGRESS);

    underTestWithLicenseCommit.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).installFailed("SonarQube was restarted before asynchronous installation of edition completed");
    verifyNoMoreInteractions(editionManagementState);
    verifyZeroInteractions(licenseCommit);
  }

  @Test
  public void stop_should_remove_license_if_uninstalling_and_LicenseCommit_is_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(UNINSTALL_IN_PROGRESS);

    underTestWithLicenseCommit.stop();

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .containsOnly("Removing license");
    verify(licenseCommit).delete();
    verifyNoMoreInteractions(licenseCommit);
    verify(editionManagementState).getPendingInstallationStatus();
    verifyNoMoreInteractions(editionManagementState);
  }

  @Test
  public void stop_should_log_if_uninstalling_and_LicenseCommit_is_not_available() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(UNINSTALL_IN_PROGRESS);

    underTest.stop();

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("License Manager plugin not found - cannot remove the license");
    verify(editionManagementState).getPendingInstallationStatus();
    verifyNoMoreInteractions(editionManagementState);
  }

  @Test
  public void should_commit_uninstall() {
    when(editionManagementState.getPendingInstallationStatus()).thenReturn(EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS);

    underTest.start();

    verify(editionManagementState).getPendingInstallationStatus();
    verify(editionManagementState).finalizeInstallation(null);
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
