/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.test.html;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class MultipartMessageAssert extends AbstractAssert<MultipartMessageAssert, Multipart> {
  private final Iterator<BodyPart> bodyParts;

  MultipartMessageAssert(MimeMultipart m) {
    super(m, MultipartMessageAssert.class);
    try {
      this.bodyParts = new BodyPartIterator(m, m.getCount());
    } catch (MessagingException e) {
      throw new IllegalStateException(e);
    }
  }

  public HtmlFragmentAssert isHtml() {
    isNotNull();

    Assertions
      .assertThat(bodyParts.hasNext())
      .describedAs("no more body part")
      .isTrue();

    try {
      BodyPart bodyPart = bodyParts.next();
      return new HtmlFragmentAssert((String) bodyPart.getContent());
    } catch (MessagingException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static class BodyPartIterator implements Iterator<BodyPart> {
    private final int count;
    private final MimeMultipart m;
    private int index = 0;

    public BodyPartIterator(MimeMultipart m, int count) {
      this.m = m;
      this.count = count;
    }

    @Override
    public boolean hasNext() {
      return index < count;
    }

    @Override
    public BodyPart next() {
      if (index >= count) {
        throw new NoSuchElementException("no more body part");
      }

      try {
        BodyPart next = m.getBodyPart(index);
        index++;
        return next;
      } catch (MessagingException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
