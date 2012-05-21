/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.review.workflow.function;

import org.sonar.core.review.workflow.review.Comment;
import org.sonar.core.review.workflow.review.MutableReview;
import org.sonar.core.review.workflow.review.Review;
import org.sonar.core.review.workflow.review.WorkflowContext;

import java.util.Map;

public final class CommentFunction extends Function {

  @Override
  public void doExecute(MutableReview review, Review initialReview, WorkflowContext context, Map<String, String> parameters) {
    Comment comment = review.createComment();
    comment.setMarkdownText(parameters.get("text"));
    comment.setUserId(context.getUserId());
  }

}
