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
package org.sonar.api.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

/**
 * @since 3.6
 */
public class UserQuery {

  public static final UserQuery ALL_ACTIVES = UserQuery.builder().build();

  private final Collection<String> logins;
  private final boolean includeDeactivated;

  private UserQuery(Builder builder) {
    this.logins = builder.logins;
    this.includeDeactivated = builder.includeDeactivated;
  }

  @CheckForNull
  public Collection<String> logins() {
    return logins;
  }

  public boolean includeDeactivated() {
    return includeDeactivated;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean includeDeactivated = false;
    private Collection<String> logins;

    private Builder() {
    }

    public Builder includeDeactivated() {
      this.includeDeactivated = true;
      return this;
    }

    public Builder logins(@Nullable Collection<String> logins) {
      // TODO clone logins
      this.logins = logins;
      return this;
    }

    public Builder logins(String... logins) {
      this.logins = Arrays.asList(logins);
      return this;
    }

    public UserQuery build() {
      if (logins != null && logins.size() >= 1000) {
        throw new IllegalArgumentException("Max number of logins is 1000. Got " + logins.size());
      }
      return new UserQuery(this);
    }
  }
}
