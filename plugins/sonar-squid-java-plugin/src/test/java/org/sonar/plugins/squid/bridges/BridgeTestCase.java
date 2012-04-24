/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.squid.bridges;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.squid.SquidExecutor;
import org.sonar.plugins.squid.SquidTestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class BridgeTestCase {

  protected SensorContext context;
  private static SquidExecutor executor;
  protected static Project project;

  @BeforeClass
  public static void scanStruts() throws IOException, URISyntaxException {
    RulesProfile profile = RulesProfile.create();
    CheckFactory checkFactory = AnnotationCheckFactory.create(profile, "repo", Collections.<Class> emptyList());
    executor = new SquidExecutor(true, "LOG, logger", checkFactory, Charsets.UTF_8);
    executor.scan(SquidTestUtils.getStrutsCoreSources(), Arrays.asList(SquidTestUtils.getStrutsCoreJar()));
    project = new Project("project");
  }

  @Before
  public final void saveData() {
    context = mock(SensorContext.class);
    when(context.getResource((Resource) anyObject())).thenAnswer(new Answer<Resource>() {

      public Resource answer(InvocationOnMock invocationOnMock) throws Throwable {
        return (Resource) invocationOnMock.getArguments()[0];
      }
    });
    executor.save(project, context, null);
  }
}
