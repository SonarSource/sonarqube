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
import org.sonar.server.component.ComponentType;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;
import org.sonar.core.i18n.I18n;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class WsParameterBuilderTest {

  private ComponentTypes componentTypes = mock(ComponentTypes.class);
  private static final ComponentType Q1 = ComponentType.builder("Q1").build();
  private static final ComponentType Q2 = ComponentType.builder("Q2").build();

  private I18n i18n = mock(I18n.class);
  private NewAction newAction = mock(NewAction.class);
  private NewParam newParam = mock(NewParam.class);

  @Test
  public void test_createRootQualifierParameter() {
    when(componentTypes.getRoots()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIER)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Project qualifier. Filter the results with the specified qualifier. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createRootQualifierParameter(newAction, newQualifierParameterContext(i18n, componentTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createRootQualifiersParameter() {
    when(componentTypes.getRoots()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. " +
      "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createRootQualifiersParameter(newAction, newQualifierParameterContext(i18n, componentTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createDefaultTemplateQualifierParameter() {
    when(componentTypes.getRoots()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIER)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Project qualifier. Filter the results with the specified qualifier. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createDefaultTemplateQualifierParameter(newAction, newQualifierParameterContext(i18n, componentTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createQualifiersParameter() {
    when(componentTypes.getAll()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li>"
      + "<li>Q2 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createQualifiersParameter(newAction, newQualifierParameterContext(i18n, componentTypes));

    assertThat(newParam).isNotNull();
  }

  @Test
  public void test_createQualifiersParameter_with_filter() {
    when(componentTypes.getAll()).thenReturn(asList(Q1, Q2));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setDescription(startsWith("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. "
      + "Possible values are:"
      + "<ul><li>Q1 - null</li></ul>"))).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createQualifiersParameter(newAction, newQualifierParameterContext(i18n, componentTypes), Sets.newHashSet(Q1.getQualifier()));

    assertThat(newParam).isNotNull();
  }


  @Test
  public void createQualifiersParameter_whenIgnoreIsSetToTrue_shouldNotReturnQualifier(){
    when(componentTypes.getAll()).thenReturn(asList(Q1, Q2, ComponentType.builder("Q3").setProperty("ignored", true).build()));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    when(newParam.setDescription(any())).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createQualifiersParameter(newAction, newQualifierParameterContext(i18n, componentTypes));

    verify(newParam).setPossibleValues(Sets.newHashSet(Q1.getQualifier(), Q2.getQualifier()));
  }

  @Test
  public void createQualifiersParameter_whenIgnoreIsSetToFalse_shouldReturnQualifier(){
    ComponentType q3Qualifier = ComponentType.builder("Q3").setProperty("ignored", false).build();
    when(componentTypes.getAll()).thenReturn(asList(Q1, Q2, q3Qualifier));
    when(newAction.createParam(PARAM_QUALIFIERS)).thenReturn(newParam);
    when(newParam.setPossibleValues(any(Collection.class))).thenReturn(newParam);
    when(newParam.setDescription(any())).thenReturn(newParam);
    NewParam newParam = WsParameterBuilder
      .createQualifiersParameter(newAction, newQualifierParameterContext(i18n, componentTypes));

    verify(newParam).setPossibleValues(Sets.newHashSet(Q1.getQualifier(), Q2.getQualifier(), q3Qualifier.getQualifier()));
  }

}
