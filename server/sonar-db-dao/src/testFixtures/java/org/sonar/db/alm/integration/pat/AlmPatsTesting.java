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
package org.sonar.db.alm.integration.pat;

import org.sonar.db.alm.pat.AlmPatDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;

public class AlmPatsTesting {

  public static AlmPatDto newAlmPatDto() {
    AlmPatDto almPatDto = new AlmPatDto();
    almPatDto.setAlmSettingUuid(secure().nextAlphanumeric(40));
    almPatDto.setPersonalAccessToken(secure().nextAlphanumeric(2000));
    almPatDto.setUserUuid(secure().nextAlphanumeric(40));
    return almPatDto;
  }

}
