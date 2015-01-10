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
package org.sonar.server.qualitygate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterQualityGatesTest {

  @Mock
  private QualityGates qualityGates;

  @Mock
  private LoadedTemplateDao templateDao;

  private RegisterQualityGates task;

  @Before
  public void setUp() {
    task = new RegisterQualityGates(qualityGates, templateDao);
  }

  @Test
  public void should_register_default_gate() {
    String templateType = "QUALITY_GATE";
    String templateName = "SonarQube way";
    when(templateDao.countByTypeAndKey(templateType, templateName)).thenReturn(0);
    when(qualityGates.create(templateName)).thenReturn(new QualityGateDto().setId(42L));

    task.start();

    verify(templateDao).countByTypeAndKey(templateType, templateName);
    verify(qualityGates).create(templateName);
    verify(qualityGates, times(8)).createCondition(anyLong(), anyString(), anyString(), anyString(), anyString(), anyInt());
    ArgumentCaptor<LoadedTemplateDto> templateArg = ArgumentCaptor.forClass(LoadedTemplateDto.class);
    verify(templateDao).insert(templateArg.capture());
    LoadedTemplateDto template = templateArg.getValue();
    assertThat(template.getType()).isEqualTo(templateType);
    assertThat(template.getKey()).isEqualTo(templateName);

    task.stop();
  }

  @Test
  public void should_not_register_default_gate_if_already_present() {
    String templateType = "QUALITY_GATE";
    String templateName = "SonarQube way";
    when(templateDao.countByTypeAndKey(templateType, templateName)).thenReturn(1);

    task.start();

    verify(templateDao).countByTypeAndKey(templateType, templateName);
    verifyZeroInteractions(qualityGates);
    verifyNoMoreInteractions(templateDao);
  }
}
