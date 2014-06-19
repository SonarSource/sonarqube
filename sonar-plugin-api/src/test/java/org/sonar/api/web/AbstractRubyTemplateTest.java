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
package org.sonar.api.web;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.sonar.api.utils.SonarException;

import java.net.URL;

public class AbstractRubyTemplateTest {

  @Test
  public void useCacheWhenTemplateIsInClassloader() {
    final AbstractRubyTemplate template = new AbstractRubyTemplate() {
      @Override
      protected String getTemplatePath() {
        return "/org/sonar/api/web/AbstractRubyTemplateTest/template.erb";
      }
    };

    assertThat(template.loadTemplateFromCache(), nullValue());
    assertThat(template.getTemplate(), is("ok"));
    assertThat(template.loadTemplateFromCache(), is("ok"));
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

    assertThat(template.loadTemplateFromCache(), nullValue());
    assertThat(template.getTemplate(), is("ok"));
    assertThat(template.loadTemplateFromCache(), nullValue());
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
