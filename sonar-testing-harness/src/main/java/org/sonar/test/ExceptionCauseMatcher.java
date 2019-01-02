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
package org.sonar.test;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matchers designed to be used as an argument of {@link org.junit.rules.ExpectedException#expectCause(Matcher)} such as:
 *
 * <pre>
 * expectedException.expect(VisitException.class);
 * expectedException.expectCause(hasType(IllegalArgumentException.class).andMessage("file and otherFile Components can not be the same"));
 * </pre>
 *
 * Class strongly inspired from {@code CauseMatcher} class from {@code http://blog.codeleak.pl/2014/03/junit-expectedexception-rule-beyond.html}
 */
@Immutable
public class ExceptionCauseMatcher extends TypeSafeMatcher<Throwable> {
  private static final String EXPECT_NO_MESSAGE_CONSTANT = "RQXG8QTUCXOT7HZ3APPKBKUE5";

  private final Class<? extends Throwable> type;
  @CheckForNull
  private final String expectedMessage;

  private ExceptionCauseMatcher(Class<? extends Throwable> type, @Nullable String expectedMessage) {
    this.type = type;
    this.expectedMessage = expectedMessage;
  }

  public static ExceptionCauseMatcher hasType(Class<? extends Throwable> type) {
    return new ExceptionCauseMatcher(type, null);
  }

  public ExceptionCauseMatcher andMessage(String expectedMessage) {
    return new ExceptionCauseMatcher(type, Objects.requireNonNull(expectedMessage));
  }

  public ExceptionCauseMatcher andNoMessage() {
    return new ExceptionCauseMatcher(type, EXPECT_NO_MESSAGE_CONSTANT);
  }

  @Override
  protected boolean matchesSafely(Throwable item) {
    if (!type.isAssignableFrom(item.getClass())) {
      return false;
    }
    if (expectedMessage == null) {
      return true;
    }
    if (EXPECT_NO_MESSAGE_CONSTANT.equals(expectedMessage)) {
      return item.getMessage() == null;
    }
    return item.getMessage().contains(expectedMessage);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("of type ")
      .appendValue(type);
    if (EXPECT_NO_MESSAGE_CONSTANT.equals(expectedMessage)) {
      description.appendText(" and no message");
    } else if (expectedMessage != null) {
      description.appendText(" and message ")
        .appendValue(expectedMessage);
    }
    description.appendText(" but");
  }
}
