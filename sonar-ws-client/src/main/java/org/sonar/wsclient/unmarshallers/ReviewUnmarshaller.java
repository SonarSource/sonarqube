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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Review;
import org.sonar.wsclient.services.WSUtils;

/**
 * @since 2.8
 */
public class ReviewUnmarshaller extends AbstractUnmarshaller<Review> {

  @Override
  protected Review parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();

    Review review = new Review();
    review.setId(utils.getLong(json, "id"));
    review.setCreatedAt(utils.getDateTime(json, "createdAt"));
    review.setUpdatedAt(utils.getDateTime(json, "updatedAt"));
    review.setAuthorLogin(utils.getString(json, "author"));
    review.setAssigneeLogin(utils.getString(json, "assignee"));
    review.setTitle(utils.getString(json, "title"));
    review.setStatus(utils.getString(json, "status"));
    review.setSeverity(utils.getString(json, "severity"));
    review.setResourceKee(utils.getString(json, "resource"));
    review.setLine(utils.getInteger(json, "line"));
    review.setResolution(utils.getString(json, "resolution"));
    review.setViolationId(utils.getLong(json, "violationId"));
    review.setType(utils.getString(json, "type"));

    Object comments = utils.getField(json, "comments");
    if (comments != null) {
      for (int i = 0; i < utils.getArraySize(comments); i++) {
        Object comment = utils.getArrayElement(comments, i);
        review.addComments(utils.getLong(comment, "id"), utils.getDateTime(comment, "updatedAt"), utils.getString(comment, "author"),
            utils.getString(comment, "text"));
      }
    }

    return review;
  }
}
