/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.audit;

import java.util.Random;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class AuditTesting {

  private static final Random random = new Random();

  public static AuditDto newAuditDto() {
    return newAuditDto(random.nextLong(), "operation");
  }

  public static AuditDto newAuditDto(String operation) {
    return newAuditDto(random.nextLong(), operation);
  }

  public static AuditDto newAuditDto(long createdAt) {
    return newAuditDto(createdAt, "operation");
  }

  public static AuditDto newAuditDto(long createdAt, String operation) {
    AuditDto auditDto = new AuditDto();
    auditDto.setUuid(randomAlphanumeric(20));
    auditDto.setUserUuid(randomAlphanumeric(40));
    auditDto.setUserLogin(randomAlphanumeric(40));
    auditDto.setNewValue("{ \"someKey\": \"someValue\" }");
    auditDto.setOperation(operation);
    auditDto.setCategory("category");
    auditDto.setCreatedAt(createdAt);
    return auditDto;
  }
}
