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
package org.sonar.server.computation.scm;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.Objects;

@Immutable
public final class Changeset {
  @CheckForNull
  private final String revision;
  @CheckForNull
  private final String author;
  @CheckForNull
  private final Long date;

  private Changeset(Builder builder) {
    this.revision = builder.revision;
    this.author = builder.author;
    this.date = builder.date;
  }

  public static Builder newChangesetBuilder() {
    return new Builder();
  }

  public static class Builder {
    @CheckForNull
    private String revision;
    @CheckForNull
    private String author;
    @CheckForNull
    private Long date;

    private Builder() {
      // prevents direct instantiation
    }

    public Builder setRevision(@Nullable String revision) {
      this.revision = revision;
      return this;
    }

    public Builder setAuthor(@Nullable String author) {
      this.author = author;
      return this;
    }

    public Builder setDate(@Nullable Long date) {
      this.date = date;
      return this;
    }

    public Changeset build() {
      return new Changeset(this);
    }
  }

  @CheckForNull
  public String getRevision() {
    return revision;
  }

  @CheckForNull
  public String getAuthor() {
    return author;
  }

  @CheckForNull
  public Long getDate() {
    return date;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Changeset changeset = (Changeset) o;
    return Objects.equals(revision, changeset.revision) &&
        Objects.equals(author, changeset.author) &&
        Objects.equals(date, changeset.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(revision, author, date);
  }

  @Override
  public String toString() {
    return "Changeset{" +
        "revision='" + revision + '\'' +
        ", author='" + author + '\'' +
        ", date=" + date +
        '}';
  }
}
