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
package org.sonar.server.issue.notification;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.Durations;
import org.sonar.db.user.UserDto;

import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_ASSIGNEE;

public class MyNewIssuesNotification extends NewIssuesNotification {

  public static final String MY_NEW_ISSUES_NOTIF_TYPE = "my-new-issues";

  public MyNewIssuesNotification(Durations durations, DetailsSupplier detailsSupplier) {
    super(MY_NEW_ISSUES_NOTIF_TYPE, durations, detailsSupplier);
  }

  public MyNewIssuesNotification setAssignee(@Nullable UserDto assignee) {
    if (assignee == null) {
      return this;
    }
    setFieldValue(FIELD_ASSIGNEE, assignee.getLogin());
    return this;
  }

  @CheckForNull
  public String getAssignee() {
    return getFieldValue(FIELD_ASSIGNEE);
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
