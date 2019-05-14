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
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class MimeMessageAssert extends AbstractAssert<MimeMessageAssert, MimeMessage> {

  public MimeMessageAssert(MimeMessage mimeMessage) {
    super(mimeMessage, MimeMessageAssert.class);
  }

  public static MimeMessageAssert assertThat(MimeMessage m) {
    return new MimeMessageAssert(m);
  }

  public MultipartMessageAssert isMultipart() {
    isNotNull();

    try {
      Object content = actual.getContent();
      Assertions.assertThat(content).isInstanceOf(MimeMultipart.class);
      MimeMultipart m = (MimeMultipart) content;
      return new MultipartMessageAssert(m);
    } catch (MessagingException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Convenience method for {@code isMultipart().isHtml()}.
   */
  public HtmlFragmentAssert isHtml() {
    return isMultipart()
      .isHtml();
  }

  public MimeMessageAssert hasRecipient(String userEmail) {
    isNotNull();

    try {
      Assertions
        .assertThat(actual.getHeader("To", null))
        .isEqualTo(String.format("<%s>", userEmail));
    } catch (MessagingException e) {
      throw new IllegalStateException(e);
    }

    return this;
  }

  public MimeMessageAssert subjectContains(String text) {
    isNotNull();

    try {
      Assertions.assertThat(actual.getSubject()).contains(text);
    } catch (MessagingException e) {
      throw new IllegalStateException(e);
    }

    return this;
  }

}
