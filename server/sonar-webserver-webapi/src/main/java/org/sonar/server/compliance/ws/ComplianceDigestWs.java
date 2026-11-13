/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.compliance.ws;

import java.util.List;
import org.sonar.api.server.ws.WebService;

public class ComplianceDigestWs implements WebService {

  private final List<ComplianceDigestWsAction> actions;

  public ComplianceDigestWs(List<ComplianceDigestWsAction> actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/compliance_digest");
    controller.setDescription("Generate security & quality compliance digests and audit reports");
    controller.setSince("10.7");
    actions.forEach(action -> action.define(controller));
    controller.done();
  }
}
