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
package org.sonar.auth.gitlab;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonUserTest {

  @Test
  public void test_parse() {
    GsonUser gsonUser = GsonUser.parse("""
      {
      "id": 4418804,
      "name": "Pierre Guillot",
      "username": "pierre-guillot-sonarsource",
      "state": "active",
      "avatar_url": "https://secure.gravatar.com/avatar/fe075537af1b94fd1cea160e5359e178?s=80&d=identicon",
      "web_url": "https://gitlab.com/pierre-guillot-sonarsource",
      "created_at": "2019-08-06T08:36:09.031Z",
      "bio": null,
      "location": null,
      "public_email": "",
      "skype": "",
      "linkedin": "",
      "twitter": "",
      "website_url": "",
      "organization": null,
      "last_sign_in_at": "2019-08-19T11:53:15.041Z",
      "confirmed_at": "2019-08-06T08:36:08.246Z",
      "last_activity_on": "2019-08-23",
      "email": "pierre.guillot@sonarsource.com",
      "theme_id": 1,
      "color_scheme_id": 1,
      "projects_limit": 100000,
      "current_sign_in_at": "2019-08-23T09:27:42.853Z",
      "identities": [
      {
      "provider": "github",
      "extern_uid": "50145663",
      "saml_provider_id": null
      }
      ],
      "can_create_group": true,
      "can_create_project": true,
      "two_factor_enabled": false,
      "external": false,
      "private_profile": false,
      "shared_runners_minutes_limit": 50000,
      "extra_shared_runners_minutes_limit": null
      }""");

    assertThat(gsonUser).isNotNull();
    assertThat(gsonUser.getUsername()).isEqualTo("pierre-guillot-sonarsource");
    assertThat(gsonUser.getName()).isEqualTo("Pierre Guillot");
    assertThat(gsonUser.getEmail()).isEqualTo("pierre.guillot@sonarsource.com");
  }
}
