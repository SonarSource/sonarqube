/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static com.google.common.base.Preconditions.checkArgument;

final class LoadedProfiles {
  private final Map<String, Profile> profileStatesPerProfileKey = new HashMap<>();

  public LoadedProfiles() {
    init();
  }

  public String loadProfile(String relativePathToProfile) {
    try {
      URL resource = getClass().getResource(relativePathToProfile);
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resource.openStream());

      String profileKey = null;
      String languageKey = null;

      Element documentElement = document.getDocumentElement();
      checkArgument("profile".equals(documentElement.getNodeName()), "%s is not a quality profile file. Root node is not %s", resource.toURI().toString());
      NodeList childNodes = documentElement.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node childNode = childNodes.item(i);
        if ("name".equals(childNode.getNodeName())) {
          profileKey = childNode.getChildNodes().item(0).getNodeValue();
        } else if ("language".equals(childNode.getNodeName())) {
          languageKey = childNode.getChildNodes().item(0).getNodeValue();
        }
      }
      checkArgument(profileKey != null, "Quality profile file %s is missing profile key", resource.toURI().toString());
      checkArgument(languageKey != null, "Quality profile file %s is missing language key", resource.toURI().toString());
      this.profileStatesPerProfileKey.put(profileKey, new Profile(profileKey, languageKey, relativePathToProfile));

      return profileKey;
    } catch (URISyntaxException | SAXException | IOException | ParserConfigurationException e) {
      throw new RuntimeException("Can not load quality profile " + relativePathToProfile, e);
    }
  }

  public Profile getState(String qualityProfileKey) {
    Profile profile = this.profileStatesPerProfileKey.get(qualityProfileKey);
    checkArgument(profile != null, "Quality Profile with key %s is unknown to %s", qualityProfileKey, ProjectAnalysisRule.class.getSimpleName());
    return profile;
  }

  public void reset() {
    this.profileStatesPerProfileKey.clear();
    init();
  }

  private void init() {
    this.profileStatesPerProfileKey.put(Profile.XOO_EMPTY_PROFILE.getProfileKey(), Profile.XOO_EMPTY_PROFILE);
  }
}
