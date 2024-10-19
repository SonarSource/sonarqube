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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.component.Component;

public class MutableMovedFilesRepositoryRule extends ExternalResource
  implements MutableMovedFilesRepository, BeforeEachCallback, AfterEachCallback {
  @CheckForNull
  private MutableMovedFilesRepository delegate;
  private final Set<Component> componentsWithOriginal = new HashSet<>();

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    after();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    before();
  }
  @Override
  protected void before() {
    this.delegate = new MutableMovedFilesRepositoryImpl();
    this.componentsWithOriginal.clear();
  }

  @Override
  protected void after() {
    this.delegate = null;
    this.componentsWithOriginal.clear();
  }

  @Override
  public void setOriginalFile(Component file, OriginalFile originalFile) {
    this.delegate.setOriginalFile(file, originalFile);
    this.componentsWithOriginal.add(file);
  }

  @Override
  public void setOriginalPullRequestFile(Component file, OriginalFile originalFile) {
    this.delegate.setOriginalPullRequestFile(file, originalFile);
    this.componentsWithOriginal.add(file);
  }

  @Override
  public Optional<OriginalFile> getOriginalFile(Component file) {
    return this.delegate.getOriginalFile(file);
  }

  @Override
  public Optional<OriginalFile> getOriginalPullRequestFile(Component file) {
    return this.delegate.getOriginalPullRequestFile(file);
  }

  public Set<Component> getComponentsWithOriginal() {
    return componentsWithOriginal;
  }
}
