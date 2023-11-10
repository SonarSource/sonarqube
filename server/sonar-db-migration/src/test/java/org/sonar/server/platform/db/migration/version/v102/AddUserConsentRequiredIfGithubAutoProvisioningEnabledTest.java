/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.server.platform.db.migration.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v102.AddUserConsentRequiredIfGithubAutoProvisioningEnabled.PROP_KEY;
import static org.sonar.server.platform.db.migration.version.v102.AddUserConsentRequiredIfGithubAutoProvisioningEnabled.PROVISIONING_GITHUB_ENABLED_PROP_KEY;

public class AddUserConsentRequiredIfGithubAutoProvisioningEnabledTest {

  @Rule
  public LogTester logger = new LogTester();

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddUserConsentRequiredIfGithubAutoProvisioningEnabled.class);
  private final DataChange underTest = new AddUserConsentRequiredIfGithubAutoProvisioningEnabled(db.database(), new System2(), UuidFactoryFast.getInstance());

  @Before
  public void before() {
    logger.clear();
  }

  @Test
  public void migration_whenGitHubAutoProvisioningPropertyNotPresent_shouldNotRequireConsent() throws SQLException {
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).isEmpty();
    assertThat(isConsentRequired()).isFalse();
  }

  @Test
  public void migration_whenGitHubAutoProvisioningDisabled_shouldNotRequireConsent() throws SQLException {
    disableGithubProvisioning();
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).isEmpty();
    assertThat(isConsentRequired()).isFalse();
  }

  @Test
  public void migration_whenGitHubAutoProvisioningEnabled_shouldRequireConsent() throws SQLException {
    enableGithubProvisioning();

    underTest.execute();

    assertThat(logger.logs(Level.WARN)).containsExactly("Automatic synchronization was previously activated for GitHub. It requires user consent to continue working as new"
                                                        + "  features were added with the synchronization. Please read the upgrade notes.");
    assertThat(isConsentRequired()).isTrue();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    enableGithubProvisioning();

    underTest.execute();
    underTest.execute();

    assertThat(logger.logs(Level.WARN)).containsExactly("Automatic synchronization was previously activated for GitHub. It requires user consent to continue working as new"
                                                        + "  features were added with the synchronization. Please read the upgrade notes.");
    assertThat(isConsentRequired()).isTrue();
  }

  private void disableGithubProvisioning() {
    toggleGithubProvisioning(false);
  }
  private void enableGithubProvisioning() {
    toggleGithubProvisioning(true);
  }

  private boolean isConsentRequired() {
    return db.countSql("select count(*) from properties where prop_key = '" + PROP_KEY + "'") >= 1;
  }

  private void toggleGithubProvisioning(boolean enabled) {
    db.executeInsert("internal_properties", "kee", PROVISIONING_GITHUB_ENABLED_PROP_KEY, "text_value", String.valueOf(enabled), "is_empty", true, "created_at", 0);
  }
}
