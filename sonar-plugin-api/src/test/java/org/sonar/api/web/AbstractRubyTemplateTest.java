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
package org.sonar.api.web;

import org.junit.Test;
import org.sonar.api.utils.SonarException;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractRubyTemplateTest {

  @Test
  public void useCacheWhenTemplateIsInClassloader() {
    final AbstractRubyTemplate template = new AbstractRubyTemplate() {
      @Override
      protected String getTemplatePath() {
        return "/org/sonar/api/web/AbstractRubyTemplateTest/template.erb";
      }
    };

    assertThat(template.loadTemplateFromCache()).isNull();
    assertThat(template.getTemplate()).isEqualTo("ok");
    assertThat(template.loadTemplateFromCache()).isEqualTo("ok");
  }

  @Test
  public void doNotCacheWhenAbsolutePath() {
    final AbstractRubyTemplate template = new AbstractRubyTemplate() {
      @Override
      protected String getTemplatePath() {
        final URL url = AbstractRubyTemplateTest.class.getResource("/org/sonar/api/web/AbstractRubyTemplateTest/template.erb");
        return url.getPath();
      }
    };

    assertThat(template.loadTemplateFromCache()).isNull();
    assertThat(template.getTemplate()).isEqualTo("ok");
    assertThat(template.loadTemplateFromCache()).isNull();
  }

  @Test(expected = SonarException.class)
  public void failIfTemplateNotFound() {
    final AbstractRubyTemplate template = new AbstractRubyTemplate() {
      @Override
      protected String getTemplatePath() {
        return "/unknown.erb";
      }
    };

    template.getTemplate();
  }
}
