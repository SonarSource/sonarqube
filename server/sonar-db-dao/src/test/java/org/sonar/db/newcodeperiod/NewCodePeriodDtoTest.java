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

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;

class NewCodePeriodDtoTest {

  @Test
  void getters_and_setters() {
    long currentTime = System2.INSTANCE.now();
    NewCodePeriodDto newCodePeriodDto = new NewCodePeriodDto()
      .setUuid("uuid")
      .setProjectUuid("projectUuid")
      .setBranchUuid("branchUuid")
      .setCreatedAt(currentTime)
      .setUpdatedAt(currentTime)
      .setType(NewCodePeriodType.NUMBER_OF_DAYS)
      .setValue("1");

    assertThat(newCodePeriodDto.getUuid()).isEqualTo("uuid");
    assertThat(newCodePeriodDto.getProjectUuid()).isEqualTo("projectUuid");
    assertThat(newCodePeriodDto.getBranchUuid()).isEqualTo("branchUuid");
    assertThat(newCodePeriodDto.getCreatedAt()).isEqualTo(currentTime);
    assertThat(newCodePeriodDto.getUpdatedAt()).isEqualTo(currentTime);
    assertThat(newCodePeriodDto.getType()).isEqualTo(NewCodePeriodType.NUMBER_OF_DAYS);
    assertThat(newCodePeriodDto.getValue()).isEqualTo("1");
  }
}
