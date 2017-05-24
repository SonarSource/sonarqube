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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkState;

public final class BuiltInQProfileCreationRule extends ExternalResource implements BuiltInQProfileCreation {
  private final List<CallLog> callLogs = new ArrayList<>();
  private List<List<ActiveRuleChange>> changesPerCall = null;
  private Iterator<List<ActiveRuleChange>> changesPerCallIterator = null;

  @Override
  protected void before() throws Throwable {
    callLogs.clear();
    changesPerCall = null;
    changesPerCallIterator = null;
  }

  @Override
  public void create(DbSession session, BuiltInQProfile qualityProfile, OrganizationDto organization, List<ActiveRuleChange> changes) {
    callLogs.add(new CallLog(qualityProfile, organization));

    if (changesPerCallIterator == null) {
      changesPerCallIterator = changesPerCall == null ? Collections.<List<ActiveRuleChange>>emptyList().iterator() : changesPerCall.iterator();
    }
    changes.addAll(changesPerCallIterator.next());
  }

  public BuiltInQProfileCreationRule addChanges(ActiveRuleChange... changes) {
    checkState(changesPerCallIterator == null, "Can't add changes if BuiltInQProfileCreation is in use");
    if (changesPerCall == null) {
      changesPerCall = new ArrayList<>();
    }
    changesPerCall.add(Arrays.asList(changes));
    return this;
  }

  public List<CallLog> getCallLogs() {
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
