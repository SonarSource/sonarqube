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
package org.sonar.db.user;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

public abstract class SearchPermissionQuery {

  public static final String ANY = "ANY";
  public static final String IN = "IN";
  public static final String OUT = "OUT";
  public static final Set<String> AVAILABLE_MEMBERSHIPS = Set.of(ANY, IN, OUT);

  protected String query;
  protected String membership;

  // for internal use in MyBatis
  protected String querySql;
  protected String querySqlLowercase;

  public String getMembership() {
    return membership;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public abstract static class Builder<T extends Builder<T>> {
    private String query;
    private String membership;

    public String getQuery(){
      return query;
    }

    public T setQuery(@Nullable String s) {
      this.query = StringUtils.defaultIfBlank(s, null);
      return self();
    }

    public String getMembership(){
      return membership;
    }

    public T setMembership(@Nullable String membership) {
      this.membership = membership;
      return self();
    }

    public void initMembership() {
      membership = firstNonNull(membership, ANY);
      checkArgument(AVAILABLE_MEMBERSHIPS.contains(membership),
        "Membership is not valid (got " + membership + "). Availables values are " + AVAILABLE_MEMBERSHIPS);
    }

    @SuppressWarnings("unchecked")
    final T self() {
      return (T) this;
    }
  }
}
