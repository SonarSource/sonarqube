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
package org.sonar.api.workflow.function;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.api.workflow.Comment;
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.api.workflow.internal.DefaultWorkflowContext;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class CommentFunctionTest {
  @Test
  public void setTextAndUserId() {
    CommentFunction function = new CommentFunction();
    Map<String, String> parameters = Maps.newHashMap();
    parameters.put("text", "foo");
    DefaultReview review = new DefaultReview();
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    context.setUserId(1234L);

    function.doExecute(review, new DefaultReview(), context, parameters);

    List<Comment> newComments = review.getNewComments();
    assertThat(newComments).hasSize(1);
    assertThat(newComments.get(0).getMarkdownText()).isEqualTo("foo");
    assertThat(newComments.get(0).getUserId()).isEqualTo(1234L);
  }
}
