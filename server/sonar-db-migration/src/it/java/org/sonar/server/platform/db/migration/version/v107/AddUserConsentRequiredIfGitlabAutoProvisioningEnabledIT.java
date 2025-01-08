/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v107.AddUserConsentRequiredIfGitlabAutoProvisioningEnabled.PROP_KEY;
import static org.sonar.server.platform.db.migration.version.v107.AddUserConsentRequiredIfGitlabAutoProvisioningEnabled.PROVISIONING_GITLAB_ENABLED_PROP_KEY;

class AddUserConsentRequiredIfGitlabAutoProvisioningEnabledIT {
  @RegisterExtension
  public final LogTesterJUnit5 logger = new LogTesterJUnit5();

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddUserConsentRequiredIfGitlabAutoProvisioningEnabled.class);
  private final DataChange underTest = new AddUserConsentRequiredIfGitlabAutoProvisioningEnabled(db.database(), new System2(), UuidFactoryImpl.INSTANCE);

  @BeforeEach
  public void before() {
    logger.clear();
  }

  @Test
  void migration_whenGitlabAutoProvisioningPropertyNotPresent_shouldNotRequireConsent() throws SQLException {
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).isEmpty();
    assertThat(isConsentRequired()).isFalse();
  }

  @Test
  void migration_whenGitlabAutoProvisioningDisabled_shouldNotRequireConsent() throws SQLException {
    disableGitlabProvisioning();
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).isEmpty();
    assertThat(isConsentRequired()).isFalse();
  }

  @Test
  void migration_whenGitlabAutoProvisioningEnabled_shouldRequireConsent() throws SQLException {
    enableGitlabProvisioning();

    underTest.execute();

    assertThat(logger.logs(Level.WARN)).containsExactly("Automatic synchronization was previously activated for Gitlab. It requires user consent to continue working as new"
      + " features were added with the synchronization. Please read the upgrade notes.");
    assertThat(isConsentRequired()).isTrue();
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    enableGitlabProvisioning();

    underTest.execute();
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).containsExactly("Automatic synchronization was previously activated for Gitlab. It requires user consent to continue working as new"
      + " features were added with the synchronization. Please read the upgrade notes.");
    assertThat(isConsentRequired()).isTrue();
  }

  private void disableGitlabProvisioning() {
    toggleGitlabProvisioning(false);
  }
  private void enableGitlabProvisioning() {
    toggleGitlabProvisioning(true);
  }

  private boolean isConsentRequired() {
    return db.countSql("select count(*) from properties where prop_key = '" + PROP_KEY + "'") >= 1;
  }

  private void toggleGitlabProvisioning(boolean enabled) {
    db.executeInsert("properties", "prop_key", PROVISIONING_GITLAB_ENABLED_PROP_KEY, "text_value", String.valueOf(enabled), "is_empty", true, "created_at", 0, "uuid", "uuid");
  }
}