/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.ui.pageselector.client;

import com.google.gwt.core.client.JavaScriptObject;

public class PageDef extends JavaScriptObject {
  // Overlay types always have protected, zero-arg ctors

  protected PageDef() {
  }

  public final native boolean isGwt() /*-{ return this.gwt; }-*/;

  public final native boolean isDefaultTab() /*-{ return this.d; }-*/;

  public final native String getId() /*-{ return this.id; }-*/;

  public final native String getUrl() /*-{ return this.url; }-*/;

  public final native String getName() /*-{ return this.name; }-*/;

  public final native StringArray getMetrics() /*-{ return this.m || []; }-*/;

  public final native StringArray getLanguages() /*-{ return this.l || []; }-*/;

  public final native StringArray getScopes() /*-{ return this.s || []; }-*/;

  public final native StringArray getQualifiers() /*-{ return this.q || []; }-*/;

  public final boolean acceptLanguage(String language) {
    return hasValue(getLanguages(), language);
  }

  public final boolean acceptScope(String scope) {
    return hasValue(getScopes(), scope);
  }

  public final boolean acceptQualifier(String qualifier) {
    return hasValue(getQualifiers(), qualifier);
  }

  public final boolean acceptMetric(String metric) {
    StringArray metrics = getMetrics();
    for (int index = 0; index < metrics.length(); index++) {
      if (metric.equals(metrics.get(index))) {
        return true;
      }
    }
    return false;
  }


  private boolean hasValue(StringArray array, String value) {
    if (array == null || array.length() == 0) {
      return true;
    }
    if (value != null) {
      for (int index = 0; index < array.length(); index++) {
        if (value.equals(array.get(index))) {
          return true;
        }
      }
    }
    return false;
  }

}

class StringArray extends JavaScriptObject {
  protected StringArray() {
  }

  public final native int length() /*-{ return this.length; }-*/;

  public final native String get(int i) /*-{ return this[i];     }-*/;
}