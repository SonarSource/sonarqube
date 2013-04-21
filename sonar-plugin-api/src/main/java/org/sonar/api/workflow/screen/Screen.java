/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.workflow.screen;

import com.google.common.annotations.Beta;

/**
 * <h2>Localization</h2>
 * <p>At least two buttons must have labels :</p>
 * <ul>
 * <li>the button in the violation toolbar that displays the form screen. Key is 'reviews.command.<command_key>.button'.</li>
 * <li>the button in the form screen that submits the command. Key is 'reviews.command.<command_key>.submit'.</li>
 * </ul>
 * @since 3.1
 */
@Beta
public abstract class Screen {
  private final String key;
  private String commandKey;

  protected Screen(String key) {
    this.key = key;
  }

  public final String getKey() {
    return key;
  }

  public final String getCommandKey() {
    return commandKey;
  }

  public final Screen setCommandKey(String commandKey) {
    this.commandKey = commandKey;
    return this;
  }
}
