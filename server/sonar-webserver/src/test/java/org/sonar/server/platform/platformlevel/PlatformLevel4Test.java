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
package org.sonar.server.platform.platformlevel;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.history.IssueCountHistoryExecutorImpl;
import org.sonar.server.history.IssueCountHistoryQGChangeEventListener;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.platform.WebCoreExtensionsInstaller;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformLevel4Test {

  @ParameterizedTest
  @CsvSource({"true,true", "false,true", "false,false"})
  void registers_issue_count_history_recording_on_every_web_node(boolean standalone, boolean startupLeader) {
    var parentContainer = mock(SpringComponentContainer.class);
    var container = mock(SpringComponentContainer.class);
    var parent = mock(PlatformLevel.class);
    var node = mock(NodeInformation.class);
    var coreExtensionsInstaller = mock(WebCoreExtensionsInstaller.class);
    var extensionInstaller = mock(ServerExtensionInstaller.class);
    when(parent.getContainer()).thenReturn(parentContainer);
    when(parentContainer.createChild()).thenReturn(container);
    when(parent.getOptional(NodeInformation.class)).thenReturn(Optional.of(node));
    when(node.isStandalone()).thenReturn(standalone);
    when(node.isStartupLeader()).thenReturn(startupLeader);
    when(parent.get(WebCoreExtensionsInstaller.class)).thenReturn(coreExtensionsInstaller);
    when(parent.get(ServerExtensionInstaller.class)).thenReturn(extensionInstaller);

    new PlatformLevel4(parent, List.of()).configure();

    verify(container).add(HistoryDbClient.class);
    verify(container).add(IssueCountHistoryRecordingService.class);
    verify(container).add(IssueCountHistoryExecutorImpl.class);
    verify(container).add(IssueCountHistoryQGChangeEventListener.class);
  }
}
