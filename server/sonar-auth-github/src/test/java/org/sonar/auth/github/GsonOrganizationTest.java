/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.auth.github;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonOrganizationTest {

  @Test
  public void parse() {
    List<GsonOrganization> underTest = GsonOrganization.parse("""
      [
        {
          "login": "github",
          "id": 1,
          "node_id": "MDEyOk9yZ2FuaXphdGlvbjE=",
          "url": "https://api.github.com/orgs/github",
          "repos_url": "https://api.github.com/orgs/github/repos",
          "events_url": "https://api.github.com/orgs/github/events",
          "hooks_url": "https://api.github.com/orgs/github/hooks",
          "issues_url": "https://api.github.com/orgs/github/issues",
          "members_url": "https://api.github.com/orgs/github/members{/member}",
          "public_members_url": "https://api.github.com/orgs/github/public_members{/member}",
          "avatar_url": "https://github.com/images/error/octocat_happy.gif",
          "description": "A great organization"
        },
        {
          "login": "github-org2",
          "id": 1,
          "node_id": "MDEyOk9yZ2FuaXphdGlvbjE=",
          "url": "https://api.github.com/orgs/github",
          "repos_url": "https://api.github.com/orgs/github/repos",
          "events_url": "https://api.github.com/orgs/github/events",
          "hooks_url": "https://api.github.com/orgs/github/hooks",
          "issues_url": "https://api.github.com/orgs/github/issues",
          "members_url": "https://api.github.com/orgs/github/members{/member}",
          "public_members_url": "https://api.github.com/orgs/github/public_members{/member}",
          "avatar_url": "https://github.com/images/error/octocat_happy.gif",
          "description": "An even greater organization"
        }
      ]""");
    assertThat(underTest).hasSize(2);
    assertThat(underTest).extracting(GsonOrganization::organization).containsExactly("github", "github-org2");
  }

}
