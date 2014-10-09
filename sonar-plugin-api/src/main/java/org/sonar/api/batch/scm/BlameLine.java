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
package org.sonar.api.batch.scm;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

/**
 * @since 5.0
 */
public class BlameLine {

  private Date date;
  private String revision;
  private String author;
  private String committer;

  /**
   * @param date of the commit
   * @param revision of the commit
   * @param author will also be used as committer identification
   */
  public BlameLine(@Nullable Date date, String revision, @Nullable String author) {
    this(date, revision, author, author);
  }

  /**
   *
   * @param date of the commit
   * @param revision of the commit
   * @param author the person who wrote the line
   * @param committer the person who committed the change
   */
  public BlameLine(@Nullable Date date, String revision, @Nullable String author, @Nullable String committer) {
    if (date != null) {
      this.date = new Date(date.getTime());
    } else {
      this.date = null;
    }
    this.revision = revision;
    this.author = author;
    this.committer = committer;
  }

  public String revision() {
    return revision;
  }

  @CheckForNull
  public String author() {
    return author;
  }

  @CheckForNull
  public String committer() {
    return committer;
  }

  /**
   * @return the commit date
   */
  @CheckForNull
  public Date date() {
    if (date != null) {
      return (Date) date.clone();
    }
    return null;
  }

  // For testing purpose

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    BlameLine rhs = (BlameLine) obj;
    return new EqualsBuilder()
      .append(date, rhs.date)
      .append(revision, rhs.revision)
      .append(author, rhs.author)
      .append(committer, rhs.committer)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(27, 45).
      append(date)
      .append(revision)
      .append(author)
      .append(committer)
      .toHashCode();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
