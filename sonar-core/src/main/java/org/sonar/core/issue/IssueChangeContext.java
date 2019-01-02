/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

public class IssueChangeContext implements Serializable {

  private final String userUuid;
  private final Date date;
  private final boolean scan;

  private IssueChangeContext(@Nullable String userUuid, Date date, boolean scan) {
    this.userUuid = userUuid;
    this.date = date;
    this.scan = scan;
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

  public static IssueChangeContext createScan(Date date) {
    return new IssueChangeContext(null, date, true);
  }

  public static IssueChangeContext createUser(Date date, @Nullable String userUuid) {
    return new IssueChangeContext(userUuid, date, false);
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
    return scan == that.scan &&
      Objects.equals(userUuid, that.userUuid) &&
      Objects.equals(date, that.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userUuid, date, scan);
  }

  @Override
  public String toString() {
    return "IssueChangeContext{" +
      "userUuid='" + userUuid + '\'' +
      ", date=" + date +
      ", scan=" + scan +
      '}';
  }
}
