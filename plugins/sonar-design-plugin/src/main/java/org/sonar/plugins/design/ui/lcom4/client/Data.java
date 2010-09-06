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
package org.sonar.plugins.design.ui.lcom4.client;

import com.google.gwt.core.client.JavaScriptObject;

public final class Data {

  public static class Entity extends JavaScriptObject {
    // Overlay types always have protected, zero-arg ctors
    protected Entity() {
    }

    public final native String getName()  /*-{ return this.n;  }-*/;
    public final native String getQualifier()  /*-{ return this.q;  }-*/;
  }

  public static class Block extends JavaScriptObject {
    protected Block() {
    }
    public final native int size() /*-{ return this.length; }-*/;
    public final native Entity get(int i) /*-{ return this[i];     }-*/;
  }

  public static class Blocks extends JavaScriptObject {
    protected Blocks() {
    }
    public final native int size() /*-{ return this.length; }-*/;
    public final native Block get(int i) /*-{ return this[i];     }-*/;
  }

  public static native Blocks parse(String json) /*-{
    return eval('(' + json + ')')
  }-*/;
}
