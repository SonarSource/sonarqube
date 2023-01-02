/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.scanner.postjob.PostJobOptimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostJobExtensionDictionaryTest {
  private final PostJobOptimizer postJobOptimizer = mock(PostJobOptimizer.class);

  @Before
  public void setUp() {
    when(postJobOptimizer.shouldExecute(any(DefaultPostJobDescriptor.class))).thenReturn(true);
  }

  @Test
  public void dependsUponPhaseForPostJob() {
    PrePostJob pre = new PrePostJob();
    NormalPostJob normal = new NormalPostJob();

    ExtensionContainer iocContainer = mock(ExtensionContainer.class);
    when(iocContainer.getComponentsByType(PostJob.class)).thenReturn(List.of(pre, normal));

    PostJobExtensionDictionary selector = new PostJobExtensionDictionary(iocContainer, postJobOptimizer, mock(PostJobContext.class));
    assertThat(selector.selectPostJobs()).extracting("wrappedPostJob").containsExactly(pre, normal);
  }

  static class NormalPostJob implements PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }

  }

  @Phase(name = Phase.Name.PRE) static
  class PrePostJob implements PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }

  }
}
