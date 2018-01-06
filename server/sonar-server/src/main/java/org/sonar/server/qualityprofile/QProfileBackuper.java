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
package org.sonar.server.qualityprofile;

import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;

/**
 * Backup and restore a Quality profile.
 */
public interface QProfileBackuper {

  void backup(DbSession dbSession, QProfileDto profile, Writer backupWriter);

  /**
   * Restore backup on a profile in the specified organization. The parameter {@code overriddenProfileName}
   * is the name of the profile to be used. If the parameter is null, then the name is loaded from the backup.
   * The profile is created if it does not exist.
   */
  QProfileRestoreSummary restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName);

  /**
   * Restore backup on an existing profile.
   */
  QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile);
}
