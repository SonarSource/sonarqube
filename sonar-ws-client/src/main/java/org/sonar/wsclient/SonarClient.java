/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient;

import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.DefaultActionPlanClient;
import org.sonar.wsclient.issue.DefaultIssueClient;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.user.DefaultUserClient;
import org.sonar.wsclient.user.UserClient;

/**
 * @since 3.6
 */
public class SonarClient {

  private final HttpRequestFactory requestFactory;

  private SonarClient(Builder builder) {
    requestFactory = new HttpRequestFactory(builder.url, builder.login, builder.password);
  }

  public IssueClient issueClient() {
    return new DefaultIssueClient(requestFactory);
  }

  public ActionPlanClient actionPlanClient() {
    return new DefaultActionPlanClient(requestFactory);
  }

  public UserClient userClient() {
    return new DefaultUserClient(requestFactory);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String login, password, url;

    private Builder() {
    }

    public Builder login(String login) {
      this.login = login;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public SonarClient build() {
      if (url == null || "".equals(url)) {
        throw new IllegalStateException("Server URL must be set");
      }
      return new SonarClient(this);
    }
  }
}
