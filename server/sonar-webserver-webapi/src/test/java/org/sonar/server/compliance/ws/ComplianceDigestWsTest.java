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
import org.junit.Test;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ComplianceDigestWsTest {

  @Test
  public void define_registersControllerAndActions() {
    ComplianceDigestWsAction action1 = mock(ComplianceDigestWsAction.class);
    ComplianceDigestWsAction action2 = mock(ComplianceDigestWsAction.class);

    ComplianceDigestWs ws = new ComplianceDigestWs(List.of(action1, action2));
    WebService.Context context = new WebService.Context();

    ws.define(context);

    WebService.Controller controller = context.controller("api/compliance_digest");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isEqualTo("Generate security & quality compliance digests and audit reports");
    assertThat(controller.since()).isEqualTo("10.7");

    verify(action1).define(controller);
    verify(action2).define(controller);
  }
}
