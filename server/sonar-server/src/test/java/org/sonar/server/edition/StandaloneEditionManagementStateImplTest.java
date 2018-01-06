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

import java.util.Collections;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.edition.EditionManagementState.PendingStatus;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.AUTOMATIC_READY;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.MANUAL_IN_PROGRESS;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS;

public class StandaloneEditionManagementStateImplTest {
  private static final License LICENSE_WITHOUT_PLUGINS = new License(randomAlphanumeric(3), Collections.emptyList(), randomAlphanumeric(10));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Nullable
  private String nullableErrorMessage = new Random().nextBoolean() ? null : randomAlphanumeric(5);
  private String errorMessage = randomAlphanumeric(5);
  private DbClient dbClient = dbTester.getDbClient();
  private final StandaloneEditionManagementStateImpl underTest = new StandaloneEditionManagementStateImpl(dbClient);

  @Test
  public void getCurrentEditionKey_fails_with_ISE_if_start_has_not_been_called() {
    expectISENotStarted();

    underTest.getCurrentEditionKey();
  }

  @Test
  public void getCurrentEditionKey_returns_empty_when_internal_properties_table_is_empty() {
    underTest.start();

    assertThat(underTest.getCurrentEditionKey()).isEmpty();
  }

  @Test
  public void getCurrentEditionKey_returns_value_in_db_for_key_currentEditionKey() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("currentEditionKey", value);
    underTest.start();

    assertThat(underTest.getCurrentEditionKey()).contains(value);
  }

  @Test
  public void getCurrentEditionKey_returns_empty_when_value_in_db_is_empty_for_key_currentEditionKey() {
    dbTester.properties().insertEmptyInternal("currentEditionKey");
    underTest.start();

    assertThat(underTest.getCurrentEditionKey()).isEmpty();
  }

  @Test
  public void getPendingInstallationStatus_fails_with_ISE_if_start_has_not_been_called() {
    expectISENotStarted();

    underTest.getPendingInstallationStatus();
  }

  @Test
  public void getPendingInstallationStatus_returns_NONE_when_internal_properties_table_is_empty() {
    underTest.start();

    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
  }

  @Test
  public void getPendingInstallationStatus_returns_value_in_db_for_key_pendingInstallStatus() {
    PendingStatus value = PendingStatus.values()[new Random().nextInt(PendingStatus.values().length)];
    dbTester.properties().insertInternal("pendingInstallStatus", value.name());
    underTest.start();

    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(value);
  }

  @Test
  public void getPendingInstallationStatus_returns_NONE_when_value_in_db_is_empty_for_key_pendingInstallStatus() {
    dbTester.properties().insertEmptyInternal("pendingInstallStatus");
    underTest.start();

    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
  }

  @Test
  public void start_fails_when_value_in_db_for_key_pendingInstallStatus_cannot_be_parsed_to_enum() {
    String value = randomAlphanumeric(30);
    dbTester.properties().insertInternal("pendingInstallStatus", value);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("No enum constant org.sonar.server.edition.EditionManagementState.PendingStatus." + value);

    underTest.start();
  }

  @Test
  public void getPendingEditionKey_fails_with_ISE_if_start_has_not_been_called() {
    expectISENotStarted();

    underTest.getPendingEditionKey();
  }

  @Test
  public void getPendingEditionKey_returns_empty_when_internal_properties_table_is_empty() {
    underTest.start();

    assertThat(underTest.getPendingEditionKey()).isEmpty();
  }

  @Test
  public void getPendingEditionKey_returns_value_in_db_for_key_pendingEditionKey() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("pendingEditionKey", value);
    underTest.start();

    assertThat(underTest.getPendingEditionKey()).contains(value);
  }

  @Test
  public void getPendingEditionKey_returns_empty_when_value_in_db_is_empty_for_key_pendingEditionKey() {
    dbTester.properties().insertEmptyInternal("pendingEditionKey");
    underTest.start();

    assertThat(underTest.getPendingEditionKey()).isEmpty();
  }

  @Test
  public void getPendingLicense_fails_with_ISE_if_start_has_not_been_called() {
    expectISENotStarted();

    underTest.getPendingLicense();
  }

  @Test
  public void getPendingLicense_returns_empty_when_internal_properties_table_is_empty() {
    underTest.start();

    assertThat(underTest.getPendingLicense()).isEmpty();
  }

  @Test
  public void getPendingLicense_returns_empty_value_in_db_for_key_pendingLicense() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("pendingLicense", value);
    underTest.start();

    assertThat(underTest.getPendingLicense()).contains(value);
  }

  @Test
  public void getPendingLicense_returns_empty_when_value_in_db_is_empty_for_key_pendingLicense() {
    dbTester.properties().insertEmptyInternal("pendingLicense");
    underTest.start();

    assertThat(underTest.getPendingLicense()).isEmpty();
  }

  @Test
  public void getInstallErrorMessage_fails_with_ISE_if_start_has_not_been_called() {
    expectISENotStarted();

    underTest.getInstallErrorMessage();
  }

  @Test
  public void getInstallErrorMessage_returns_empty_when_internal_properties_table_is_empty() {
    underTest.start();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void getInstallErrorMessage_returns_value_in_db_for_key_pendingEditionKey() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("installError", value);
    underTest.start();

    assertThat(underTest.getInstallErrorMessage()).contains(value);
  }

  @Test
  public void getInstallErrorMessage_returns_empty_when_value_in_db_is_empty_for_key_pendingEditionKey() {
    dbTester.properties().insertEmptyInternal("installError");
    underTest.start();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startAutomaticInstall_fails_with_NPE_if_license_is_null() {
    underTest.start();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("license can't be null");

    underTest.startAutomaticInstall(null);
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_after_start() {
    underTest.start();

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_after_start_with_existing_currentEditionKey() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("currentEditionKey", value);
    underTest.start();

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(value);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_after_newEditionWithoutInstall() {
    String value = randomAlphanumeric(10);
    underTest.start();
    underTest.newEditionWithoutInstall(value);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(value);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_after_finalizeInstallation() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();
    underTest.finalizeInstallation(null);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_and_clears_error_message_after_finalizeInstallation() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.finalizeInstallation(errorMessage);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_fails_with_ISE_if_called_after_startAutomaticInstall() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_IN_PROGRESS when status is AUTOMATIC_IN_PROGRESS (should be any of [NONE])");

    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startAutomaticInstall_fails_with_ISE_if_called_after_automaticInstallReady() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_IN_PROGRESS when status is AUTOMATIC_READY (should be any of [NONE])");

    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startAutomaticInstall_fails_with_ISE_if_called_after_manualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_IN_PROGRESS when status is MANUAL_IN_PROGRESS (should be any of [NONE])");

    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startAutomaticInstall_fails_with_ISE_if_called_after_uninstall() {
    underTest.start();
    underTest.newEditionWithoutInstall("foo");
    underTest.uninstall();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_IN_PROGRESS when status is UNINSTALL_IN_PROGRESS (should be any of [NONE])");

    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_after_installFailed() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(null);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_and_clears_error_message_after_installFailed_with_message() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(errorMessage);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_after_finalizeInstall() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();
    underTest.finalizeInstallation(null);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startAutomaticInstall_sets_pending_fields_and_clears_error_message_after_finalizeInstall_with_message() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.finalizeInstallation(errorMessage);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void uninstall_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.uninstall();
  }

  @Test
  public void uninstall_resets_fields_after_start_and_install() {
    String value = randomAlphanumeric(10);
    underTest.start();
    underTest.newEditionWithoutInstall(value);

    PendingStatus newStatus = underTest.uninstall();

    assertThat(newStatus).isEqualTo(UNINSTALL_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(UNINSTALL_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void uninstall_fails_with_ISE_if_called_after_uninstall() {
    String value = randomAlphanumeric(10);
    underTest.start();
    underTest.newEditionWithoutInstall(value);
    underTest.uninstall();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to UNINSTALL_IN_PROGRESS when status is UNINSTALL_IN_PROGRESS (should be any of [NONE])");

    underTest.uninstall();
  }

  @Test
  public void uninstall_resets_fields_after_newEditionWithoutInstall() {
    String value = randomAlphanumeric(10);
    underTest.start();
    underTest.newEditionWithoutInstall(value);

    PendingStatus newStatus = underTest.uninstall();

    assertThat(newStatus).isEqualTo(UNINSTALL_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(UNINSTALL_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void uninstall_fails_with_ISE_if_called_after_startAutomaticInstall() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to UNINSTALL_IN_PROGRESS when status is AUTOMATIC_IN_PROGRESS (should be any of [NONE])");

    underTest.uninstall();
  }

  @Test
  public void uninstall_fails_with_ISE_if_called_after_automaticInstallReady() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to UNINSTALL_IN_PROGRESS when status is AUTOMATIC_READY (should be any of [NONE])");

    underTest.uninstall();
  }

  @Test
  public void uninstall_fails_with_ISE_if_called_after_manualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to UNINSTALL_IN_PROGRESS when status is MANUAL_IN_PROGRESS (should be any of [NONE])");

    underTest.uninstall();
  }

  @Test
  public void startAutomaticInstall_succeeds_if_called_after_installFailed_and_clears_errorMessage() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(errorMessage);

    PendingStatus newStatus = underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startManualInstall_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startManualInstall_fails_with_NPE_if_license_is_null() {
    underTest.start();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("license can't be null");

    underTest.startManualInstall(null);
  }

  @Test
  public void startManualInstall_sets_pending_fields_after_start() {
    underTest.start();

    PendingStatus newStatus = underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startManualInstall_sets_pending_fields_after_start_with_existing_currentEditionKey() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("currentEditionKey", value);
    underTest.start();

    PendingStatus newStatus = underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(value);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startManualInstall_sets_pending_fields_after_newEditionWithoutInstall() {
    String value = randomAlphanumeric(10);
    underTest.start();
    underTest.newEditionWithoutInstall(value);

    PendingStatus newStatus = underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).contains(value);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void startManualInstall_fails_with_ISE_if_called_after_startAutomaticInstall() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to MANUAL_IN_PROGRESS when status is AUTOMATIC_IN_PROGRESS (should be any of [NONE])");

    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startManualInstall_fails_with_ISE_if_called_after_automaticInstallReady() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to MANUAL_IN_PROGRESS when status is AUTOMATIC_READY (should be any of [NONE])");

    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startManualInstall_fails_with_ISE_if_called_after_manualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to MANUAL_IN_PROGRESS when status is MANUAL_IN_PROGRESS (should be any of [NONE])");

    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startManualInstall_fails_with_ISE_if_called_after_uninstall() {
    underTest.start();
    underTest.newEditionWithoutInstall("foo");
    underTest.uninstall();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to MANUAL_IN_PROGRESS when status is UNINSTALL_IN_PROGRESS (should be any of [NONE])");

    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
  }

  @Test
  public void startManualInstall_succeeds_if_called_after_installFailed_and_clears_errorMessage() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(errorMessage);

    PendingStatus newStatus = underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    assertThat(newStatus).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(MANUAL_IN_PROGRESS);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.automaticInstallReady();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_called() {
    underTest.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_READY when status is NONE (should be any of [AUTOMATIC_IN_PROGRESS])");

    underTest.automaticInstallReady();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_called_after_manualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_READY when status is MANUAL_IN_PROGRESS (should be any of [AUTOMATIC_IN_PROGRESS])");

    underTest.automaticInstallReady();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_called_after_newEditionWithoutInstall() {
    underTest.start();
    underTest.newEditionWithoutInstall("foo");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_READY when status is NONE (should be any of [AUTOMATIC_IN_PROGRESS])");

    underTest.automaticInstallReady();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_called_after_uninstall() {
    underTest.start();
    underTest.newEditionWithoutInstall("foo");
    underTest.uninstall();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_READY when status is UNINSTALL_IN_PROGRESS (should be any of [AUTOMATIC_IN_PROGRESS])");

    underTest.automaticInstallReady();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_called_after_startManualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_READY when status is MANUAL_IN_PROGRESS (should be any of [AUTOMATIC_IN_PROGRESS])");

    underTest.automaticInstallReady();
  }

  @Test
  public void automaticInstallReady_after_startAutomaticInstall_changes_status_to_AUTOMATIC_READY_but_does_not_change_editions() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.automaticInstallReady();

    assertThat(newStatus).isEqualTo(AUTOMATIC_READY);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(AUTOMATIC_READY);
    assertThat(underTest.getPendingEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getPendingLicense()).contains(LICENSE_WITHOUT_PLUGINS.getContent());
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void automaticInstallReady_fails_with_ISE_if_called_after_installFailed() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(nullableErrorMessage);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to AUTOMATIC_READY when status is NONE (should be any of [AUTOMATIC_IN_PROGRESS])");

    underTest.automaticInstallReady();
  }

  @Test
  public void newEditionWithoutInstall_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.newEditionWithoutInstall(randomAlphanumeric(3));
  }

  @Test
  public void newEditionWithoutInstall_fails_with_NPE_if_newEditionKey_is_null() {
    underTest.start();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("newEditionKey can't be null");

    underTest.newEditionWithoutInstall(null);
  }

  @Test
  public void newEditionWithoutInstall_fails_with_IAE_if_newEditionKey_is_empty() {
    underTest.start();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("newEditionKey can't be empty");

    underTest.newEditionWithoutInstall("");
  }

  @Test
  public void newEditionWithoutInstall_changes_current_edition() {
    String newEditionKey = randomAlphanumeric(3);
    underTest.start();

    PendingStatus newStatus = underTest.newEditionWithoutInstall(newEditionKey);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(newEditionKey);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void newEditionWithoutInstall_overwrite_current_edition() {
    String newEditionKey = randomAlphanumeric(3);
    underTest.start();

    PendingStatus newStatus = underTest.newEditionWithoutInstall(newEditionKey);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(newEditionKey);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void newEditionWithoutInstall_overwrites_from_previous_newEditionWithoutInstall() {
    String newEditionKey1 = randomAlphanumeric(3);
    String newEditionKey2 = randomAlphanumeric(3);
    underTest.start();
    underTest.newEditionWithoutInstall(newEditionKey1);

    PendingStatus newStatus = underTest.newEditionWithoutInstall(newEditionKey2);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(newEditionKey2);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void newEditionWithoutInstall_fails_with_ISE_if_called_after_startAutomaticInstall() {
    String newEditionKey = randomAlphanumeric(3);
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is AUTOMATIC_IN_PROGRESS (should be any of [NONE])");

    underTest.newEditionWithoutInstall(newEditionKey);
  }

  @Test
  public void newEditionWithoutInstall_fails_with_ISE_if_called_after_startManualInstall() {
    String newEditionKey = randomAlphanumeric(3);
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is MANUAL_IN_PROGRESS (should be any of [NONE])");

    underTest.newEditionWithoutInstall(newEditionKey);
  }

  @Test
  public void newEditionWithoutInstall_fails_with_ISE_if_called_after_uninstall() {
    String newEditionKey = randomAlphanumeric(3);
    underTest.start();
    underTest.newEditionWithoutInstall("foo");
    underTest.uninstall();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is UNINSTALL_IN_PROGRESS (should be any of [NONE])");

    underTest.newEditionWithoutInstall(newEditionKey);
  }

  @Test
  public void newEditionWithoutInstall_succeeds_if_called_after_installFailed_and_clear_error_message() {
    String newEditionKey = randomAlphanumeric(3);
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(errorMessage);

    PendingStatus newStatus = underTest.newEditionWithoutInstall(newEditionKey);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(newEditionKey);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void finalizeInstallation_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.finalizeInstallation(nullableErrorMessage);
  }

  @Test
  public void finalizeInstallation_fails_with_ISE_after_start() {
    underTest.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is NONE (should be any of [AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS])");

    underTest.finalizeInstallation(nullableErrorMessage);
  }

  @Test
  public void finalizeInstallation_fails_with_ISE_after_newEditionWithoutInstall() {
    underTest.start();
    underTest.newEditionWithoutInstall(randomAlphanumeric(3));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is NONE (should be any of [AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS])");

    underTest.finalizeInstallation(nullableErrorMessage);
  }

  @Test
  public void finalizeInstallation_fails_with_ISE_after_startAutomaticInstall() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is AUTOMATIC_IN_PROGRESS (should be any of [AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS])");

    underTest.finalizeInstallation(nullableErrorMessage);
  }

  @Test
  public void finalizeInstallation_set_new_edition_and_clear_pending_fields_after_manualInstallationReady() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    PendingStatus newStatus = underTest.finalizeInstallation(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void finalizeInstallation_set_new_edition_and_clear_pending_fields_and_sets_error_message_after_manualInstallationReady() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    PendingStatus newStatus = underTest.finalizeInstallation(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void finalizeInstallation_set_new_edition_and_clear_pending_fields_after_startManualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.finalizeInstallation(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void finalizeInstallation_set_new_edition_and_clear_pending_fields_and_sets_error_message_after_startManualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.finalizeInstallation(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void finalizeInstallation_overwrites_current_edition_and_clear_pending_fields_after_startManualInstall() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("currentEditionKey", value);
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.finalizeInstallation(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void finalizeInstallation_overwrites_current_edition_and_clear_pending_fields_and_sets_error_message_after_startManualInstall() {
    String value = randomAlphanumeric(10);
    dbTester.properties().insertInternal("currentEditionKey", value);
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.finalizeInstallation(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(LICENSE_WITHOUT_PLUGINS.getEditionKey());
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void finalizeInstallation_fails_with_ISE_after_installFailed() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(nullableErrorMessage);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is NONE (should be any of [AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS])");

    underTest.finalizeInstallation(nullableErrorMessage);
  }

  @Test
  public void finalizeInstallation_set_new_edition_and_clear_pending_fields_after_uninstall() {
    underTest.start();
    String value = randomAlphanumeric(10);
    underTest.newEditionWithoutInstall(value);
    underTest.uninstall();

    PendingStatus newStatus = underTest.finalizeInstallation(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void finalizeInstallation_set_new_edition_and_clear_pending_fields_and_sets_error_message_after_uninstall() {
    underTest.start();
    String value = randomAlphanumeric(10);
    underTest.newEditionWithoutInstall(value);
    underTest.uninstall();

    PendingStatus newStatus = underTest.finalizeInstallation(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void finalizeInstallation_fails_with_ISE_after_finalizeInstallation() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.finalizeInstallation(nullableErrorMessage);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is NONE (should be any of [AUTOMATIC_READY, MANUAL_IN_PROGRESS, UNINSTALL_IN_PROGRESS])");

    underTest.finalizeInstallation(nullableErrorMessage);
  }

  @Test
  public void installFailed_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.installFailed(nullableErrorMessage);
  }

  @Test
  public void installFailed_fails_with_ISE_if_called_after_start() {
    underTest.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is NONE (should be any of [AUTOMATIC_IN_PROGRESS, MANUAL_IN_PROGRESS])");

    underTest.installFailed(nullableErrorMessage);
  }

  @Test
  public void installFailed_after_manualInstall_changes_status_to_NONE_stores_non_null_error_message_and_clear_pending_fields() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);
    String errorMessage = randomAlphanumeric(4);

    PendingStatus newStatus = underTest.installFailed(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void installFailed_after_manualInstall_changes_status_to_NONE_without_error_message_and_clear_pending_fields() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.installFailed(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void installFailed_after_startAutomaticInstall_changes_status_to_NONE_stores_non_null_error_message_and_clear_pending_fields() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    String errorMessage = randomAlphanumeric(4);

    PendingStatus newStatus = underTest.installFailed(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void installFailed_after_startAutomaticInstall_changes_status_to_NONE_without_error_message_and_clear_pending_fields() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.installFailed(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).isEmpty();
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void installFailed_after_startAutomaticInstall_changes_status_to_NONE_stores_non_null_error_message_and_clear_pending_fields_when_current_install_exists() {
    underTest.start();
    String currentEdition = "current-edition";
    underTest.newEditionWithoutInstall(currentEdition);
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    String errorMessage = randomAlphanumeric(4);

    PendingStatus newStatus = underTest.installFailed(errorMessage);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(currentEdition);
    assertThat(underTest.getInstallErrorMessage()).contains(errorMessage);
  }

  @Test
  public void installFailed_after_startAutomaticInstall_changes_status_to_NONE_without_error_message_and_clear_pending_fields_when_current_install_exists() {
    underTest.start();
    String currentEdition = "current-edition";
    underTest.newEditionWithoutInstall(currentEdition);
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    PendingStatus newStatus = underTest.installFailed(null);

    assertThat(newStatus).isEqualTo(NONE);
    assertThat(underTest.getPendingInstallationStatus()).isEqualTo(NONE);
    assertThat(underTest.getPendingEditionKey()).isEmpty();
    assertThat(underTest.getPendingLicense()).isEmpty();
    assertThat(underTest.getCurrentEditionKey()).contains(currentEdition);
    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void installFailed_considers_empty_message_as_no_message() {
    underTest.start();
    underTest.newEditionWithoutInstall("current-edition");
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    underTest.installFailed("");

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void installFailed_considers_empty_message_after_trim_as_no_message() {
    underTest.start();
    underTest.newEditionWithoutInstall("current-edition");
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    underTest.installFailed("  ");

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void installFailed_fails_with_ISE_after_automaticInstallReady() {
    underTest.start();
    String currentEdition = "current-edition";
    underTest.newEditionWithoutInstall(currentEdition);
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is AUTOMATIC_READY (should be any of [AUTOMATIC_IN_PROGRESS, MANUAL_IN_PROGRESS])");

    underTest.installFailed(nullableErrorMessage);
  }

  @Test
  public void installFailed_fails_with_ISE_after_installFailed() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(nullableErrorMessage);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can't move to NONE when status is NONE (should be any of [AUTOMATIC_IN_PROGRESS, MANUAL_IN_PROGRESS])");

    underTest.installFailed(nullableErrorMessage);
  }

  private void expectISENotStarted() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("StandaloneEditionManagementStateImpl is not started");
  }

  @Test
  public void clearInstallErrorMessage_fails_with_ISE_if_not_started() {
    expectISENotStarted();

    underTest.clearInstallErrorMessage();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_state() {
    underTest.start();

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_manualInstall() {
    underTest.start();
    underTest.startManualInstall(LICENSE_WITHOUT_PLUGINS);

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_automaticInstall() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_automaticInstallReady() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.automaticInstallReady();

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_automaticInstallFailed_and_clears_message() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(errorMessage);

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_automaticInstallFailed_without_message() {
    underTest.start();
    underTest.startAutomaticInstall(LICENSE_WITHOUT_PLUGINS);
    underTest.installFailed(null);

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

  @Test
  public void clearInstallErrorMessage_succeeds_after_newEditionWithoutInstall() {
    underTest.start();
    underTest.newEditionWithoutInstall(randomAlphanumeric(4));

    underTest.clearInstallErrorMessage();

    assertThat(underTest.getInstallErrorMessage()).isEmpty();
  }

}
