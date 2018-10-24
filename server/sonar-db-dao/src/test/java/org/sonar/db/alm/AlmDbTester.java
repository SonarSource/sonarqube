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

package org.sonar.db.alm;

import java.util.Arrays;
import java.util.function.Consumer;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.sonar.db.alm.ALM.GITHUB;

public class AlmDbTester {

  private final DbTester db;

  public AlmDbTester(DbTester db) {
    this.db = db;
  }

  public OrganizationAlmBindingDto insertOrganizationAlmBinding(OrganizationDto organization, AlmAppInstallDto almAppInstall) {
    UserDto user = db.users().insertUser();
    db.getDbClient().organizationAlmBindingDao().insert(db.getSession(), organization, almAppInstall, randomAlphabetic(10), user.getUuid());
    db.commit();
    return db.getDbClient().organizationAlmBindingDao().selectByOrganization(db.getSession(), organization).get();
  }

  @SafeVarargs
  public final AlmAppInstallDto insertAlmAppInstall(Consumer<AlmAppInstallDto>... dtoPopulators) {
    AlmAppInstallDto dto = new AlmAppInstallDto()
      .setAlmId(GITHUB.getId())
      .setInstallId(randomAlphanumeric(10))
      .setOwnerId(randomAlphanumeric(10))
      .setIsOwnerUser(false)
      .setUserExternalId(randomAlphanumeric(10));
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(dto));
    db.getDbClient().almAppInstallDao().insertOrUpdate(db.getSession(), dto.getAlm(), dto.getOwnerId(), dto.isOwnerUser(), dto.getInstallId(), dto.getUserExternalId());
    db.commit();
    return db.getDbClient().almAppInstallDao().selectByOwnerId(db.getSession(), dto.getAlm(), dto.getOwnerId()).get();
  }

}
