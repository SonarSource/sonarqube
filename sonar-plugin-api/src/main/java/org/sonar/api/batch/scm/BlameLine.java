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
  public BlameLine(Date date, String revision, String author) {
    this(date, revision, author, author);
  }

  /**
   *
   * @param date of the commit
   * @param revision of the commit
   * @param author the person who wrote the line
   * @param committer the person who committed the change
   */
  public BlameLine(Date date, String revision, String author, String committer) {
    setDate(date);
    setRevision(revision);
    setAuthor(author);
    setCommitter(committer);
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getCommitter() {
    return committer;
  }

  public void setCommitter(String committer) {
    this.committer = committer;
  }

  /**
   * @return the commit date
   */
  public Date getDate() {
    if (date != null)
    {
      return (Date) date.clone();
    }
    return null;
  }

  public void setDate(Date date) {
    if (date != null)
    {
      this.date = new Date(date.getTime());
    }
    else
    {
      this.date = null;
    }
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
