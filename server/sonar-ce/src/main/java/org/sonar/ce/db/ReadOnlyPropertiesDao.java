/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.db;

import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

/**
 * Compute Engine specific override of {@link PropertiesDao} and {@link org.sonar.db.property.PropertiesDao} which
 * implements no write method (ie. insert/update/delete) because updating the Properties is the Web Server responsibility
 * alone.
 * <p>
 * This ugly trick is required because licensed plugin bundle {@link com.sonarsource.license.api.internal.ServerLicenseVerifierImpl}
 * which update license properties by calling {@link PropertiesDao} directly and this can not be disabled.
 * </p>
 */
public class ReadOnlyPropertiesDao extends PropertiesDao {
  public ReadOnlyPropertiesDao(MyBatis mybatis, System2 system2, UuidFactory uuidFactory) {
    super(mybatis, system2, uuidFactory, new NoOpAuditPersister());
  }

  @Override
  public void saveProperty(DbSession session, PropertyDto property, @Nullable String userLogin,
    @Nullable String projectKey, @Nullable String projectName, @Nullable String qualifier) {
    // do nothing
  }

  @Override
  public void saveProperty(PropertyDto property) {
    // do nothing
  }

  @Override
  public void deleteProjectProperty(DbSession session, String key, String projectUuid, String projectKey,
    String projectName, String qualifier) {
    // do nothing
  }

  @Override
  public void deleteGlobalProperty(String key, DbSession session) {
    // do nothing
  }

  @Override
  public void renamePropertyKey(String oldKey, String newKey) {
    // do nothing
  }

}
