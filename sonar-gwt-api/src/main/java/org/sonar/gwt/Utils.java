/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.gwt;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

public final class Utils {

  private Utils() {
    // only static methods
  }

  /**
   * @return width in pixels of the GWT component in the Sonar page
   */
  public static int getPageWidth() {
    return DOM.getElementById("gwtpage").getClientWidth();
  }
  
  public static String escapeHtml(String maybeHtml) {
    final Element div = DOM.createDiv();
    DOM.setInnerText(div, maybeHtml);
    return DOM.getInnerHTML(div);
  }

  public static String formatPercent(String percentage) {
    return percentage == null || percentage.equals("") ? "" : formatPercent(new Double(percentage));
  }

  public static String formatPercent(double percentage) {
    return NumberFormat.getFormat("0.0").format(percentage) + "%";
  }

  public static String formatNumber(String number) {
    return number == null || number.equals("") ? "" : formatNumber(new Double(number));
  }

  public static String formatNumber(double number) {
    return NumberFormat.getDecimalFormat().format(number);
  }

  public static native void showError(String message) /*-{
    $wnd.error(message);
  }-*/;

  public static native void showWarning(String message) /*-{
    $wnd.warning(message);
  }-*/;

  public static native void showInfo(String message) /*-{
    $wnd.info(message);
  }-*/;
}
