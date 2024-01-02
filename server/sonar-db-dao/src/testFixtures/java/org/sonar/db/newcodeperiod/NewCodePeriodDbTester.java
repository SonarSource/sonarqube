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
package org.sonar.db.newcodeperiod;

import javax.annotation.Nullable;
import org.sonar.db.DbTester;

public class NewCodePeriodDbTester {

  private final DbTester dbTester;

  public NewCodePeriodDbTester(DbTester dbTester) {
    this.dbTester = dbTester;
  }

  public NewCodePeriodDto insert(NewCodePeriodDto dto) {
    dbTester.getDbClient().newCodePeriodDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
    return dto;
  }

  public NewCodePeriodDto insert(@Nullable String projectUuid, @Nullable String branchUuid, NewCodePeriodType type, @Nullable String value) {
    NewCodePeriodDto dto = new NewCodePeriodDto()
      .setProjectUuid(projectUuid)
      .setBranchUuid(branchUuid)
      .setType(type)
      .setValue(value);
    insert(dto);
    return dto;
  }

  public NewCodePeriodDto insert(@Nullable String projectUuid, NewCodePeriodType type, @Nullable String value) {
    NewCodePeriodDto dto = new NewCodePeriodDto()
      .setProjectUuid(projectUuid)
      .setType(type)
      .setValue(value);
    insert(dto);
    return dto;
  }

  public NewCodePeriodDto insert(NewCodePeriodType type, @Nullable String value) {
    NewCodePeriodDto dto = new NewCodePeriodDto()
      .setType(type)
      .setValue(value);
    insert(dto);
    return dto;
  }

}
