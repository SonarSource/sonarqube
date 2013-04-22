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
package org.sonar.core.review;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Collections2;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.Collection;

public class ReviewDao implements BatchComponent, ServerComponent {
  private final MyBatis mybatis;
  private final Cache<Long, Collection<ReviewDto>> cacheByResource;

  public ReviewDao(MyBatis mybatis) {
    this.mybatis = mybatis;
    this.cacheByResource = CacheBuilder.newBuilder()
        .weakValues()
        .build(new CacheLoader<Long, Collection<ReviewDto>>() {
          @Override
          public Collection<ReviewDto> load(Long resourceId) {
            return doSelectOpenByResourceId(resourceId);
          }
        });
  }

  /**
   * @since 3.1
   */
  public ReviewDto findById(long reviewId) {
    SqlSession session = mybatis.openSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      return mapper.findById(reviewId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<ReviewDto> selectOpenByResourceId(long resourceId, @Nullable Predicate<ReviewDto>... predicates) {
    Collection<ReviewDto> reviews = cacheByResource.getUnchecked(resourceId);
    if (!reviews.isEmpty() && predicates != null) {
      reviews = Collections2.filter(reviews, Predicates.and(predicates));
    }
    return reviews;
  }

  public Collection<ReviewDto> selectOnDeletedResources(long rootProjectId, long rootSnapshotId) {
    SqlSession session = mybatis.openSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      return mapper.selectOnDeletedResources(rootProjectId, rootSnapshotId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private Collection<ReviewDto> doSelectOpenByResourceId(long resourceId) {
    SqlSession session = mybatis.openSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      return mapper.selectByResourceId(resourceId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ReviewDao update(Collection<ReviewDto> reviews) {
    Preconditions.checkNotNull(reviews);

    SqlSession session = mybatis.openBatchSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      for (ReviewDto review : reviews) {
        mapper.update(review);
      }
      session.commit();
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
