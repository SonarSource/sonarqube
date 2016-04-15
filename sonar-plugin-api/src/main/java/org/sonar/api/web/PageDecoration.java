/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.web;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * Adds content to HTML pages. A PageDecoration is a Rails template (html.erb file) that executes content_for blocks on predefined locations :
 * <ul>
 *   <li><code>script</code> : javascript header</li>
 *   <li><code>style</code> : CSS header</li>
 *   <li><code>header</code> : area over the black top navigation bar</li>
 *   <li><code>footer</code> : area below the main page</li>
 *   <li><code>sidebar</code> : area in the sidebar between the menu and the sonar logo</li>
 * </ul>
 *
 * <p>Example of template: 
<pre>
 &lt;% content_for :script do %&gt;
   &lt;script&gt;alert('page loaded')&lt;/script&gt;
 &lt;% end %&gt;

 &lt;% content_for :footer do %&gt;
  &lt;div&gt;this is &lt;b&gt;my footer&lt;/b&gt;&lt;/div&gt;
&lt;% end %&gt;
</pre>
 *
 * @since 3.3
 */
@ServerSide
@ExtensionPoint
public abstract class PageDecoration extends AbstractRubyTemplate {

}
