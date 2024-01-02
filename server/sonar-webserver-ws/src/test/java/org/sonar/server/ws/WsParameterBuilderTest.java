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
package org.sonar.server.ws;

import com.google.common.collect.Sets;
import java.util.Collection;
import org.junit.Test;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;
import org.sonar.core.i18n.I18n;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class WsParameterBuilderTest {

  private ResourceTypes resourceTypes = mock(ResourceTypes.class);
  private static final ResourceType Q1 = ResourceType.builder("Q1").build();
  private static final ResourceType Q2 = ResourceType.builder("Q2").build();

  private I18n i18n = mock(I18n.class);
  private NewAction newAction = mock(NewAction.class);
  private NewParam newParam = mock(NewParam.class);

  @Test
  public void test_createRootQualifierParameter() {
    when(resourceTypes.getRoots()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIER)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Project qualifier. Filter the results with the specified qualifier. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createRootQualifierParameter(newAction, newQualifierParameterContext(i18n, resourceTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createRootQualifiersParameter() {
    when(resourceTypes.getRoots()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. " +
      "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createRootQualifiersParameter(newAction, newQualifierParameterContext(i18n, resourceTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createDefaultTemplateQualifierParameter() {
    when(resourceTypes.getRoots()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIER)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Project qualifier. Filter the results with the specified qualifier. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createDefaultTemplateQualifierParameter(newAction, newQualifierParameterContext(i18n, resourceTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createQualifiersParameter() {
    when(resourceTypes.getAll()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createQualifiersParameter(newAction, newQualifierParameterContext(i18n, resourceTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createQualifiersParameter_with_filter() {
    when(resourceTypes.getAll()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createQualifiersParameter(newAction, newQualifierParameterContext(i18n, resourceTypes), Sets.newHashSet(Q1.getQualifier()));

    assertThat(newParam).isNotNull();
  }

}
