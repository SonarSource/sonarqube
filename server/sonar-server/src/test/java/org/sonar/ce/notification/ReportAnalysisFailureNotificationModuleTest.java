/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.notification;

import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.notification.NotificationDispatcherMetadata;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportAnalysisFailureNotificationModuleTest {
  private ReportAnalysisFailureNotificationModule underTest = new ReportAnalysisFailureNotificationModule();

  @Test
  public void adds_dispatcher_and_its_metadata() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters(NotificationDispatcherMetadata.class)).isNotNull();
    assertThat(container.getPicoContainer().getComponentAdapters(ReportAnalysisFailureNotificationDispatcher.class)).isNotNull();
  }

  @Test
  public void adds_template_and_serializer() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters(ReportAnalysisFailureNotificationEmailTemplate.class)).isNotNull();
    assertThat(container.getPicoContainer().getComponentAdapters(ReportAnalysisFailureNotificationSerializer.class)).isNotNull();
  }
}
