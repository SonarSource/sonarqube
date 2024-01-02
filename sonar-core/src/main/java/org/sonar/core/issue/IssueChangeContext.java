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
package org.sonar.core.issue;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class IssueChangeContext implements Serializable {

  private final String userUuid;
  private final Date date;
  private final boolean scan;
  private final boolean refreshMeasures;
  private final String externalUser;
  private final String webhookSource;

  private IssueChangeContext(Date date, boolean scan, boolean refreshMeasures, @Nullable String userUuid, @Nullable String externalUser,
    @Nullable String webhookSource) {
    this.userUuid = userUuid;
    this.date = requireNonNull(date);
    this.scan = scan;
    this.refreshMeasures = refreshMeasures;
    this.externalUser = externalUser;
    this.webhookSource = webhookSource;
  }

  @CheckForNull
  public String userUuid() {
    return userUuid;
  }

  public Date date() {
    return date;
  }

  public boolean scan() {
    return scan;
  }

  public boolean refreshMeasures() {
    return refreshMeasures;
  }

  @Nullable
  public String getExternalUser() {
    return externalUser;
  }

  @Nullable
  public String getWebhookSource() {
    return webhookSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueChangeContext that = (IssueChangeContext) o;
    return scan == that.scan && refreshMeasures == that.refreshMeasures &&  Objects.equals(userUuid, that.userUuid) && date.equals(that.date)
      && Objects.equals(externalUser, that.getExternalUser()) && Objects.equals(webhookSource, that.getWebhookSource());
  }

  @Override
  public int hashCode() {
    return Objects.hash(userUuid, date, scan, refreshMeasures, externalUser, webhookSource);
  }

  public static IssueChangeContextBuilder newBuilder() {
    return new IssueChangeContextBuilder();
  }

  public static IssueChangeContextBuilder issueChangeContextByScanBuilder(Date date) {
    return newBuilder().withScan().setUserUuid(null).setDate(date);
  }

  public static IssueChangeContextBuilder issueChangeContextByUserBuilder(Date date, @Nullable String userUuid) {
    return newBuilder().setUserUuid(userUuid).setDate(date);
  }

  public static final class IssueChangeContextBuilder {
    private String userUuid;
    private Date date;
    private boolean scan = false;
    private boolean refreshMeasures = false;
    private String externalUser;
    private String webhookSource;

    private IssueChangeContextBuilder() {
    }

    public IssueChangeContextBuilder setUserUuid(@Nullable String userUuid) {
      this.userUuid = userUuid;
      return this;
    }

    public IssueChangeContextBuilder setDate(Date date) {
      this.date = date;
      return this;
    }

    public IssueChangeContextBuilder withScan() {
      this.scan = true;
      return this;
    }

    public IssueChangeContextBuilder withRefreshMeasures() {
      this.refreshMeasures = true;
      return this;
    }

    public IssueChangeContextBuilder setExternalUser(@Nullable String externalUser) {
      this.externalUser = externalUser;
      return this;
    }

    public IssueChangeContextBuilder setWebhookSource(@Nullable String webhookSource) {
      this.webhookSource = webhookSource;
      return this;
    }

    public IssueChangeContext build() {
      return new IssueChangeContext(date, scan, refreshMeasures, userUuid, externalUser, webhookSource);
    }
  }
}
