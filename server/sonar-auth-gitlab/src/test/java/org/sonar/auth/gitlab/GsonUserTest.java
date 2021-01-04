/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.gitlab;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonUserTest {

  @Test
  public void test_parse() {
    GsonUser gsonUser = GsonUser.parse("{\n" +
      "\"id\": 4418804,\n" +
      "\"name\": \"Pierre Guillot\",\n" +
      "\"username\": \"pierre-guillot-sonarsource\",\n" +
      "\"state\": \"active\",\n" +
      "\"avatar_url\": \"https://secure.gravatar.com/avatar/fe075537af1b94fd1cea160e5359e178?s=80&d=identicon\",\n" +
      "\"web_url\": \"https://gitlab.com/pierre-guillot-sonarsource\",\n" +
      "\"created_at\": \"2019-08-06T08:36:09.031Z\",\n" +
      "\"bio\": null,\n" +
      "\"location\": null,\n" +
      "\"public_email\": \"\",\n" +
      "\"skype\": \"\",\n" +
      "\"linkedin\": \"\",\n" +
      "\"twitter\": \"\",\n" +
      "\"website_url\": \"\",\n" +
      "\"organization\": null,\n" +
      "\"last_sign_in_at\": \"2019-08-19T11:53:15.041Z\",\n" +
      "\"confirmed_at\": \"2019-08-06T08:36:08.246Z\",\n" +
      "\"last_activity_on\": \"2019-08-23\",\n" +
      "\"email\": \"pierre.guillot@sonarsource.com\",\n" +
      "\"theme_id\": 1,\n" +
      "\"color_scheme_id\": 1,\n" +
      "\"projects_limit\": 100000,\n" +
      "\"current_sign_in_at\": \"2019-08-23T09:27:42.853Z\",\n" +
      "\"identities\": [\n" +
      "{\n" +
      "\"provider\": \"github\",\n" +
      "\"extern_uid\": \"50145663\",\n" +
      "\"saml_provider_id\": null\n" +
      "}\n" +
      "],\n" +
      "\"can_create_group\": true,\n" +
      "\"can_create_project\": true,\n" +
      "\"two_factor_enabled\": false,\n" +
      "\"external\": false,\n" +
      "\"private_profile\": false,\n" +
      "\"shared_runners_minutes_limit\": 50000,\n" +
      "\"extra_shared_runners_minutes_limit\": null\n" +
      "}");

    assertThat(gsonUser).isNotNull();
    assertThat(gsonUser.getUsername()).isEqualTo("pierre-guillot-sonarsource");
    assertThat(gsonUser.getName()).isEqualTo("Pierre Guillot");
    assertThat(gsonUser.getEmail()).isEqualTo("pierre.guillot@sonarsource.com");
  }
}
