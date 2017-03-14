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
package org.sonar.server.qualityprofile;

import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;

/**
 * Backup and restore a Quality profile.
 */
public interface QProfileBackuper {

  void backup(DbSession dbSession, QualityProfileDto profile, Writer backupWriter);

  /**
   * Restore a profile backup in the specified organization. The parameter {@code overriddenProfileName}
   * is the name of the profile to be used. If null then name is loaded from backup.
   */
  BulkChangeResult restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName);

}
