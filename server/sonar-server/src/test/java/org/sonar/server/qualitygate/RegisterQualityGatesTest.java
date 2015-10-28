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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGatesTest {

  static long QGATE_ID = 42L;

  QualityGates qualityGates = mock(QualityGates.class);
  LoadedTemplateDao templateDao = mock(LoadedTemplateDao.class);
  RegisterQualityGates task = new RegisterQualityGates(qualityGates, templateDao);

  @Test
  public void register_default_gate() {
    String templateType = "QUALITY_GATE";
    String templateName = "SonarQube way";
    when(templateDao.countByTypeAndKey(templateType, templateName)).thenReturn(0);
    when(qualityGates.create(templateName)).thenReturn(new QualityGateDto().setId(QGATE_ID));

    task.start();

    verify(templateDao).countByTypeAndKey(templateType, templateName);
    verify(qualityGates).create(templateName);
    verify(qualityGates).createCondition(eq(QGATE_ID), eq(NEW_BLOCKER_VIOLATIONS_KEY), eq(OPERATOR_GREATER_THAN), eq((String) null), eq("0"), eq(1));
    verify(qualityGates).createCondition(eq(QGATE_ID), eq(NEW_CRITICAL_VIOLATIONS_KEY), eq(OPERATOR_GREATER_THAN), eq((String) null), eq("0"), eq(1));
    verify(qualityGates).createCondition(eq(QGATE_ID), eq(NEW_SQALE_DEBT_RATIO_KEY), eq(OPERATOR_GREATER_THAN), eq((String) null), eq("5"), eq(1));
    verify(qualityGates).createCondition(eq(QGATE_ID), eq(NEW_COVERAGE_KEY), eq(OPERATOR_LESS_THAN), eq((String) null), eq("80"), eq(1));
    verify(qualityGates).setDefault(eq(QGATE_ID));

    ArgumentCaptor<LoadedTemplateDto> templateArg = ArgumentCaptor.forClass(LoadedTemplateDto.class);
    verify(templateDao).insert(templateArg.capture());
    LoadedTemplateDto template = templateArg.getValue();
    assertThat(template.getType()).isEqualTo(templateType);
    assertThat(template.getKey()).isEqualTo(templateName);

    task.stop();
  }

  @Test
  public void does_not_register_default_gate_if_already_present() {
    String templateType = "QUALITY_GATE";
    String templateName = "SonarQube way";
    when(templateDao.countByTypeAndKey(templateType, templateName)).thenReturn(1);

    task.start();

    verify(templateDao).countByTypeAndKey(templateType, templateName);
    verifyZeroInteractions(qualityGates);
    verifyNoMoreInteractions(templateDao);
  }
}
