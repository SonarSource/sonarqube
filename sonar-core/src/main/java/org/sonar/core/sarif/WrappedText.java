/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class WrappedText {
  @SerializedName("text")
  private final String text;

  private WrappedText(String textToWrap) {
    this.text = textToWrap;
  }

  public static WrappedText of(String textToWrap) {
    return new WrappedText(textToWrap);
  }

  public String getText() {
    return text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WrappedText that = (WrappedText) o;
    return Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text);
  }
}
