/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.user;

/**
 * The principal a GitHub webhook call runs as, once
 * {@link org.sonar.server.authentication.GithubWebhookAuthentication} has verified its HMAC signature.
 */
public class GithubWebhookUserSession extends AbstractServiceUserSession {

  public static final String GITHUB_WEBHOOK_USER_NAME = "github-webhook";

  @Override
  public String getLogin() {
    return GITHUB_WEBHOOK_USER_NAME;
  }

}
