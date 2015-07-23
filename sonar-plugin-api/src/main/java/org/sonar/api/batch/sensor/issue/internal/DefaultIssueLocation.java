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
package org.sonar.api.batch.sensor.issue.internal;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class DefaultIssueLocation implements NewIssueLocation, IssueLocation {

  private static final String INPUT_DIR_SHOULD_BE_NON_NULL = "InputDir should be non null";
  private static final String INPUT_FILE_SHOULD_BE_NON_NULL = "InputFile should be non null";
  private static final String ON_FILE_OR_ON_DIR_ALREADY_CALLED = "onFile or onDir already called";
  private static final String ON_PROJECT_ALREADY_CALLED = "onProject already called";

  private boolean onProject = false;
  private InputPath path;
  private TextRange textRange;
  private String message;

  @Override
  public NewIssueLocation onFile(InputFile file) {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    Preconditions.checkNotNull(file, INPUT_FILE_SHOULD_BE_NON_NULL);
    this.path = file;
    return this;
  }

  @Override
  public NewIssueLocation onDir(InputDir dir) {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    Preconditions.checkNotNull(dir, INPUT_DIR_SHOULD_BE_NON_NULL);
    this.path = dir;
    return this;
  }

  @Override
  public NewIssueLocation onProject() {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    this.onProject = true;
    return this;
  }

  @Override
  public NewIssueLocation at(TextRange location) {
    Preconditions.checkState(this.path != null && this.path instanceof InputFile, "at() should be called after onFile.");
    DefaultInputFile file = (DefaultInputFile) this.path;
    file.validate(location);
    this.textRange = location;
    return this;
  }

  @Override
  public NewIssueLocation message(String message) {
    Preconditions.checkNotNull(message, "Message can't be null");
    Preconditions.checkArgument(message.length() <= MESSAGE_MAX_SIZE,
      "Message of an issue can't be greater than " + MESSAGE_MAX_SIZE + ": [" + message + "] size is " + message.length());
    this.message = message;
    return this;
  }

  @Override
  @CheckForNull
  public InputPath inputPath() {
    return this.path;
  }

  @Override
  public TextRange textRange() {
    return textRange;
  }

  @Override
  public String message() {
    return this.message;
  }

}
