/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.batch.scm;

import java.nio.file.Paths;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ScmProviderTest {

  private final ScmProvider provider = new ScmProvider() {

    @Override
    public String key() {
      return "foo";
    }
  };

  @Test
  public void default_implementation_does_not_support_blame() {
    assertThat(provider.supports(null)).isFalse();

    assertThatThrownBy(() -> provider.blameCommand())
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void default_implementation_does_not_support_relativePathFromScmRoot() {
    assertThatThrownBy(() -> provider.relativePathFromScmRoot(Paths.get("foo")))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void default_implementation_does_not_support_revisionId() {
    assertThatThrownBy(() -> provider.revisionId(Paths.get("foo")))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void default_implementation_does_not_support_ignore() {
    assertThatThrownBy(() -> provider.ignoreCommand())
      .isInstanceOf(UnsupportedOperationException.class);
  }
}
