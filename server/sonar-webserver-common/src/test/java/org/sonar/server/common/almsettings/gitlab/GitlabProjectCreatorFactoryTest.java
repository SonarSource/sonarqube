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
package org.sonar.server.common.almsettings.gitlab;

import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitlabProjectCreatorFactoryTest {

  @Mock
  private GitlabDevOpsProjectCreationContextService gitlabDevOpsProjectService;

  @InjectMocks
  private GitlabProjectCreatorFactory underTest;


  @Test
  void getDevOpsProjectCreator_withCharacteristics_returnsEmpty() {
    assertThat(underTest.getDevOpsProjectCreator(mock(DbSession.class), Map.of())).isEmpty();
  }


  @Test
  void getDevOpsProjectCreator_whenDevOpsPlatformIsNotGitlab_returnsEmpty() {
    AlmSettingDto almSetting = mockAlm(ALM.GITHUB);
    AssertionsForClassTypes.assertThat(underTest.getDevOpsProjectCreator(almSetting, Mockito.mock(DevOpsProjectDescriptor.class))).isEmpty();
  }


  @Test
  void getDevOpsProjectCreator_whenDevOpsPlatformIsGitlab_returnsProjectCreator() {
    when(gitlabDevOpsProjectService.create(any(), any())).thenReturn(mock());

    AlmSettingDto almSetting = mockAlm(ALM.GITLAB);
    DevOpsProjectDescriptor devOpsProjectDescriptor = mock();
    
    Optional<DevOpsProjectCreator> devOpsProjectCreator = underTest.getDevOpsProjectCreator(almSetting, devOpsProjectDescriptor);
    
    assertThat(devOpsProjectCreator).isNotEmpty();
    verify(gitlabDevOpsProjectService).create(almSetting, devOpsProjectDescriptor);
  }
  
  private static AlmSettingDto mockAlm(ALM alm) {
    AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    when(almSettingDto.getAlm()).thenReturn(alm);
    return almSettingDto;
  }

}
