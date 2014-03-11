/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.core.issue.workflow;

import org.junit.Test;
import org.sonar.api.user.User;
import org.sonar.core.user.DefaultUser;

import static org.mockito.Mockito.*;

public class SetAssigneeTest {
  @Test
  public void assign() throws Exception {
    User user = new DefaultUser().setLogin("eric").setName("eric");
    SetAssignee function = new SetAssignee(user);
    Function.Context context = mock(Function.Context.class);
    function.execute(context);
    verify(context, times(1)).setAssignee(user);
  }

  @Test
  public void unassign() throws Exception {
    Function.Context context = mock(Function.Context.class);
    SetAssignee.UNASSIGN.execute(context);
    verify(context, times(1)).setAssignee(null);
  }
}
