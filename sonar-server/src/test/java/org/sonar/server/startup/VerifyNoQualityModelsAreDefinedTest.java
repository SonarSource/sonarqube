/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.qualitymodel.ModelDefinition;
import org.sonar.api.utils.MessageException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class VerifyNoQualityModelsAreDefinedTest {

  @Test
  public void not_fail_if_no_model_defined() {
    // Not fail
    VerifyNoQualityModelsAreDefined verifyNoQualityModelsAreDefined = new VerifyNoQualityModelsAreDefined();
    verifyNoQualityModelsAreDefined.start();
  }

  @Test
  public void fail_if_at_least_one_model_is_defined() {
    try {
      VerifyNoQualityModelsAreDefined verifyNoQualityModelsAreDefined = new VerifyNoQualityModelsAreDefined(new ModelDefinition[]{mock(ModelDefinition.class)});
      verifyNoQualityModelsAreDefined.start();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class);
    }
  }
}
