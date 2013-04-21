/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.design.ui.page.client;

import com.google.gwt.core.client.JavaScriptObject;

public final class DsmData {

  public static class Row extends JavaScriptObject {

    // Overlay types always have protected, zero-arg ctors
    protected Row() {
    }

    public final native String getId() /*-{ return this.i; }-*/;
    public final native String getName()  /*-{ return this.n;  }-*/;
    public final native String getQualifier() /*-{ return this.q; }-*/;
    public final native Cell getCell(final int col)  /*-{ return this.v[col];  }-*/;
    public final native int size()  /*-{ return this.v.length;  }-*/;
    public final int getWeight(final int col) {
      Cell cell = getCell(col);
      return (cell==null) ? 0 : cell.getWeight();
    }
  }

  public static class Cell extends JavaScriptObject {

    // Overlay types always have protected, zero-arg ctors
    protected Cell() {
    }

    public final native String getDependencyId() /*-{ if(this.i) {return this.i;} else { return null;} }-*/;
    public final native int getWeight()  /*-{ return this.w || 0;  }-*/;
  }


  public static class Rows extends JavaScriptObject {
    protected Rows() {
    }
    public final native int size() /*-{ return this.length; }-*/;
    public final native Row get(int i) /*-{ return this[i];     }-*/;
  }

  public static native Rows parse(String json) /*-{
    return eval('(' + json + ')')
  }-*/;

}
