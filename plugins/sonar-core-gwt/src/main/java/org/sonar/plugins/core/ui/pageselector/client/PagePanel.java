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

import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import org.sonar.api.web.gwt.client.Utils;
import org.sonar.gwt.Configuration;
import org.sonar.gwt.Links;
import org.sonar.gwt.ui.Loading;

public class PagePanel extends SimplePanel {

  private PageDef def;
  private String rootPanelId;
  private String currentResourceId = null;

  public PagePanel(PageDef def) {
    this.def = def;
    rootPanelId = "gwtpage-" + def.getId();
    add(new HTML("<div id=\"" + rootPanelId + "\"> </div>"));
  }

  public void display() {
    String resourceId = Configuration.getResourceId();
    if (resourceId != null && !resourceId.equals(currentResourceId)) {
      currentResourceId = resourceId;
      if (def.isGwt()) {
        loadGwt(Links.baseUrl(), Configuration.getSonarVersion(), def.getId());
      } else {
        loadEmbeddedPage(resourceId);
      }
    }
  }

  private native void loadGwt(final String serverUrl, final String sonarVersion, final String gwtId) /*-{
    if ($wnd.modules[gwtId]!=null) {
        $wnd.modules[gwtId]();
        return;
    }

    // Create the script tag to be used for importing the GWT script loader.
    var script = $doc.createElement('script');
    script.type = 'text/javascript';
    script.src = serverUrl + '/deploy/gwt/' + gwtId + '/' + gwtId + '.nocache.js?' + sonarVersion;

    // The default GWT script loader calls document.write() twice which prevents loading scripts
    // on demand, after the document has been loaded. To overcome this we have to overwrite the document.write()
    // method before the GWT script loader is executed and restore it after.
    // NOTE: The GWT script loader uses document.write() to compute the URL from where it is loaded.
    var counter = 0;
    var limit = 2;
    var oldWrite = $doc.write;
    var newWrite = function(html) {
        if (counter < limit) {
            counter++;
            // Fail silently if the script element hasn't been attached to the document.
            if (!script.parentNode) {
                return;
            }
            // Create a DIV and put the HTML inside.
            var div = $doc.createElement('div');
            // We have to replace all the script tags because otherwise IE drops them.
            div.innerHTML = html.replace(/<script\b([\s\S]*?)<\/script>/gi, "<pre script=\"script\"$1</pre>");
            // Move DIV contents after the GWT script loader.
            var nextSibling = script.nextSibling;
            while(div.firstChild) {
                var child = div.firstChild;
                // Recover the script tags.
                if (child.nodeName.toLowerCase() == 'pre' && child.getAttribute('script') == 'script') {
                    var pre = child;
                    pre.removeAttribute('script');
                    // Create the script tag.
                    child = $doc.createElement('script');
                    // Copy all the attributes.
                    for (var i = 0; i < pre.attributes.length; i++) {
                        var attrNode = pre.attributes[i];
                        // In case of IE we have to copy only the specified attributes.
                        if (typeof attrNode.specified == 'undefined'
                            || (typeof attrNode.specified == 'boolean' && attrNode.specified)) {
                            child.setAttribute(attrNode.nodeName, attrNode.nodeValue);
                        }
                    }
                    // Copy the script text.
                    child.text = typeof pre.innerText == 'undefined' ? pre.textContent : pre.innerText;
                    // Don't forget to remove the placeholder.
                    div.removeChild(pre);
                }
                if (nextSibling) {
                    script.parentNode.insertBefore(child, nextSibling);
                } else {
                    script.parentNode.appendChild(child);
                }
            }
        }
        if (counter >= limit) {
            $doc.write = oldWrite;
            oldWrite = undefined;
            script = undefined;
            counter = undefined;
        }
    };

    // Append the script tag to the head.
    var heads = $doc.getElementsByTagName('head');
    if (heads.length > 0) {
        $doc.write = newWrite;
        heads[0].appendChild(script);
    }
  }-*/;

  private void loadEmbeddedPage(String resourceId) {
    final RootPanel panel = RootPanel.get(rootPanelId);
    panel.add(new Loading());
    String url = def.getUrl();
    if (url == null) {
      url = "/plugins/resource/" + resourceId + "?page=" + def.getId() + "&layout=false&hd=false";
    } else {
      url += resourceId;
    }
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(Links.baseUrl() + url));
    try {
      builder.sendRequest(null, new RequestCallback() {
        public void onError(Request request, Throwable exception) {
          Utils.showError("Can not load the page " + request.toString());
        }

        public void onResponseReceived(Request request, Response response) {
          panel.clear();
          panel.add(new HTML(response.getText()));
        }
      });
    } catch (RequestException e) {
      Utils.showError("Can not connect to server: " + url);
    }
  }
}
