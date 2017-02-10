/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * PLUGINS MUST NOT BE USED THIS CLASS, EXCEPT FOR UNIT TESTING.
 *
 * @since 3.6
 */
public class IssueChangeContext implements Serializable {

  private final String login;
  private final Date date;
  private final boolean scan;

  private IssueChangeContext(@Nullable String login, Date date, boolean scan) {
    this.login = login;
    this.date = date;
    this.scan = scan;
  }

  @CheckForNull
  public String login() {
    return login;
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

  public static IssueChangeContext createUser(Date date, @Nullable String login) {
    return new IssueChangeContext(login, date, false);
  }
}
