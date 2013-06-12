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
package org.sonar.batch.events;

/**
 * Generic event for some steps of project scan.
 * @since 3.7
 *
 */
public class BatchStepEvent extends BatchEvent<BatchStepHandler>
    implements BatchStepHandler.BatchStepEvent {

  private final boolean start;

  private String stepName;

  public BatchStepEvent(String stepName, boolean start) {
    this.start = start;
    this.stepName = stepName;
  }

  @Override
  public String stepName() {
    return stepName;
  }

  public final boolean isStart() {
    return start;
  }

  public final boolean isEnd() {
    return !start;
  }

  @Override
  protected void dispatch(BatchStepHandler handler) {
    handler.onBatchStep(this);
  }

  @Override
  protected Class getType() {
    return BatchStepHandler.class;
  }

}
