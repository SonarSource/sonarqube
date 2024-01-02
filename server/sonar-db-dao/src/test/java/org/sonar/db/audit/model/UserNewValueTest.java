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
package org.sonar.db.audit.model;

import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.sonar.db.user.UserDto;

import static org.junit.Assert.fail;

public class UserNewValueTest {

  private static final JSONParser jsonParser = new JSONParser();

  @Test
  public void toString_givenAllFieldsWithValue_returnValidJSON() {
    UserDto userDto = new UserDto();
    userDto.setName("name");
    userDto.setEmail("name@email.com");
    userDto.setActive(true);
    userDto.setScmAccounts("\ngithub-account\n");
    userDto.setExternalId("name");
    userDto.setExternalLogin("name");
    userDto.setExternalIdentityProvider("github");
    userDto.setLocal(false);
    userDto.setLastConnectionDate(System.currentTimeMillis());
    UserNewValue userNewValue = new UserNewValue(userDto);

    String jsonString = userNewValue.toString();

    assertValidJSON(jsonString);
  }

  @Test
  public void toString_givenUserUuidAndUserLogin_returnValidJSON() {
    UserNewValue userNewValue = new UserNewValue("userUuid", "userLogin");

    String jsonString = userNewValue.toString();

    assertValidJSON(jsonString);
  }
  
  private void assertValidJSON(String jsonString) {
    try {
      jsonParser.parse(jsonString);
    } catch (Exception e) {
      //if the json is invalid the test will fail
      fail("UserNewValue.toString() generated invalid JSON. More details: " + e.getMessage());
    }
  }
}
