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
package org.sonar.server.qualityprofile;

import java.util.ArrayList;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

public class BuiltInQProfileInsertRule extends ExternalResource implements BuiltInQProfileInsert {
  private final List<BuiltInQProfileInsertRule.CallLog> callLogs = new ArrayList<>();

  @Override
  protected void before() throws Throwable {
    callLogs.clear();
  }

  @Override
  public void create(DbSession session, DbSession batchSession, BuiltInQProfile builtInQProfile, OrganizationDto organization) {
    callLogs.add(new BuiltInQProfileInsertRule.CallLog(builtInQProfile, organization));
  }

  public List<BuiltInQProfileInsertRule.CallLog> getCallLogs() {
    return callLogs;
  }

  public static final class CallLog {
    private final BuiltInQProfile builtInQProfile;
    private final OrganizationDto organizationDto;

    private CallLog(BuiltInQProfile builtInQProfile, OrganizationDto organizationDto) {
      this.builtInQProfile = builtInQProfile;
      this.organizationDto = organizationDto;
    }

    public BuiltInQProfile getDefinedQProfile() {
      return builtInQProfile;
    }

    public OrganizationDto getOrganizationDto() {
      return organizationDto;
    }
  }
}
