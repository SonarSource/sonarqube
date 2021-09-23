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
package org.sonar.auth.bitbucket;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.Verb;
import org.sonar.api.server.ServerSide;

@ServerSide
public class BitbucketScribeApi extends DefaultApi20 {

  private final BitbucketSettings settings;

  public BitbucketScribeApi(BitbucketSettings settings) {
    this.settings = settings;
  }

  @Override
  public String getAccessTokenEndpoint() {
    return settings.webURL() + "site/oauth2/access_token";
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }

  @Override
  protected String getAuthorizationBaseUrl() {
    return settings.webURL() + "site/oauth2/authorize";
  }
}
