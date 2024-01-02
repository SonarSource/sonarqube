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
package org.sonarqube.ws.tester;

import javax.annotation.Nullable;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.client.newcodeperiods.NewCodePeriodsService;
import org.sonarqube.ws.client.newcodeperiods.SetRequest;
import org.sonarqube.ws.client.newcodeperiods.ShowRequest;
import org.sonarqube.ws.client.newcodeperiods.UnsetRequest;

public class NewCodePeriodTester {

  private final TesterSession session;

  NewCodePeriodTester(TesterSession session) {
    this.session = session;
  }

  public NewCodePeriodsService service() {
    return session.wsClient().newCodePeriods();
  }

  public void setGlobal(String type, @Nullable String value) {
    set(null, null, type, value);
  }

  public void set(@Nullable String projectKey, @Nullable String branchKey, String type, @Nullable String value) {
    session.wsClient().newCodePeriods().set(new SetRequest()
      .setProject(projectKey)
      .setBranch(branchKey)
      .setType(type)
      .setValue(value));
  }

  public void unset(@Nullable String projectKey, @Nullable String branchKey) {
    session.wsClient().newCodePeriods().unset(new UnsetRequest()
      .setProject(projectKey)
      .setBranch(branchKey));
  }

  public NewCodePeriods.ShowWSResponse show(@Nullable String projectKey, @Nullable String branchKey) {
    return session.wsClient().newCodePeriods().show(new ShowRequest()
      .setProject(projectKey)
      .setBranch(branchKey));
  }

}
