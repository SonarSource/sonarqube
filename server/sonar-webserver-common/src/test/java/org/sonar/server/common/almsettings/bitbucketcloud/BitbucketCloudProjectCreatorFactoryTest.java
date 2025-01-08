/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.almsettings.bitbucketcloud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitbucketCloudProjectCreatorFactoryTest {

  @Mock
  private DbClient dbClient;
  @Mock
  private UserSession userSession;
  @Mock
  private BitbucketCloudRestClient bitbucketCloudRestClient;
  @Mock
  private ProjectCreator projectCreator;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;

  @InjectMocks
  private BitbucketCloudProjectCreatorFactory underTest;

  @Test
  void getDevOpsProjectCreator_whenAlmIsNotBitbucketCloud_shouldReturnEmpty() {
    AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    when(almSettingDto.getAlm()).thenReturn(ALM.GITLAB);
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITLAB, null, "bitbucket_repo", "bitbucket_project");

    assertThat(underTest.getDevOpsProjectCreator(almSettingDto, devOpsProjectDescriptor)).isEmpty();
  }

  @Test
  void getDevOpsProjectCreator_whenAlmItBitbucketCloud_shouldReturnProjectCreator() {
    AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    when(almSettingDto.getAlm()).thenReturn(ALM.BITBUCKET_CLOUD);
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.BITBUCKET_CLOUD, null, "bitbucket_repo", "bitbucket_project");

    DevOpsProjectCreator expectedProjectCreator = new BitbucketCloudProjectCreator(dbClient, almSettingDto, devOpsProjectDescriptor, userSession, bitbucketCloudRestClient,
      projectCreator, projectKeyGenerator);
    DevOpsProjectCreator devOpsProjectCreator = underTest.getDevOpsProjectCreator(almSettingDto, devOpsProjectDescriptor).orElseThrow();

    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedProjectCreator);
  }

}
