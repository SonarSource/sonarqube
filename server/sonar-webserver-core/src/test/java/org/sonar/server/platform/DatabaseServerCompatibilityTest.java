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
package org.sonar.server.platform;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DatabaseServerCompatibilityTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final DatabaseVersion version = mock(DatabaseVersion.class);
  private final DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);
  private final DatabaseServerCompatibility compatibility = new DatabaseServerCompatibility(version, documentationLinkGenerator);

  @Test
  public void fail_if_requires_downgrade() {
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);

    assertThatThrownBy(compatibility::start)
      .isInstanceOf(MessageException.class)
      .hasMessage("Database was upgraded to a more recent version of SonarQube. "
        + "A backup must probably be restored or the DB settings are incorrect.");
    verifyNoInteractions(documentationLinkGenerator);
  }

  @Test
  public void fail_if_requires_firstly_to_upgrade_to_lta() {
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(version.getVersion()).thenReturn(Optional.of(12L));

    assertThatThrownBy(compatibility::start)
      .isInstanceOf(MessageException.class)
      .hasMessage("The version of SonarQube you are trying to upgrade from is too old. Please upgrade to the 9.9 Long-Term Active version first.");
    verifyNoInteractions(documentationLinkGenerator);
  }

  @Test
  public void log_warning_if_requires_upgrade() {
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(version.getVersion()).thenReturn(Optional.of(DatabaseVersion.MIN_UPGRADE_VERSION));
    when(documentationLinkGenerator.getDocumentationLink("/server-upgrade-and-maintenance/upgrade/upgrade-the-server/roadmap")).thenReturn("[expected doc url]");

    compatibility.start();

    assertThat(logTester.logs()).hasSize(4);
    assertThat(logTester.logs(Level.WARN)).contains(
      "The database must be manually upgraded. Please backup the database and browse /setup. "
        + "For more information: [expected doc url]",
      "################################################################################",
      "The database must be manually upgraded. Please backup the database and browse /setup. "
        + "For more information: [expected doc url]",
      "################################################################################");
  }

  @Test
  public void do_nothing_if_up_to_date() {
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    compatibility.start();
    // no error
  }
}
