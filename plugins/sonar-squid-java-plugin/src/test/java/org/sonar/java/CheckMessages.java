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
package org.sonar.java;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.ObjectUtils;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import com.google.common.collect.Ordering;

/**
 * Utility class to simplify assertions in tests for checks.
 * It can be used as following:
 * <pre>
 * CheckMessages = new CheckMessages(SourceFile);
 * checkMessages.assertNext().atLine(1).withMessage("Message").withCost(1.0);
 * checkMessages.assertNoMore();
 * </pre>
 * TODO Godin: I think that this class can be reused by consumers of sonar-squid, thus can be moved to test-library for sonar-squid.
 */
public class CheckMessages {

  private final List<CheckMessage> findings;
  private final Iterator<CheckMessage> findingsIterator;

  public CheckMessages(SourceFile file) {
    findings = CHECK_MESSAGE_ORDERING.immutableSortedCopy(file.getCheckMessages());
    findingsIterator = findings.iterator();
  }

  private static final Ordering<CheckMessage> CHECK_MESSAGE_ORDERING = new Ordering<CheckMessage>() {
    @Override
    public int compare(CheckMessage o1, CheckMessage o2) {
      return ObjectUtils.compare(o1.getLine(), o2.getLine());
    }
  };

  public Next assertNext() {
    assertTrue("There is no more violations", findingsIterator.hasNext());
    return new Next(findingsIterator.next());
  }

  public void assertNoMore() {
    assertFalse(findingsIterator.hasNext());
  }

  public static final class Next {
    private final CheckMessage checkMessage;

    private Next(CheckMessage checkMessage) {
      this.checkMessage = checkMessage;
    }

    public Next atLine(Integer expected) {
      assertThat(checkMessage.getLine(), is(expected));
      return this;
    }

    public Next withMessage(String expected) {
      assertThat(checkMessage.getText(Locale.getDefault()), equalTo(expected));
      return this;
    }

    public Next withCost(double expected) {
      assertThat(checkMessage.getCost(), is(expected));
      return this;
    }
  }

}
