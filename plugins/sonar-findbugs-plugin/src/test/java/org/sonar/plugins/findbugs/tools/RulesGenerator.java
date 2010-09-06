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
package org.sonar.plugins.findbugs.tools;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulesCategory;
import org.sonar.api.rules.StandardRulesXmlParser;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesGenerator {

  private static final String FOR_VERSION = "1.3.8";


  private final static Map<String, String> BUG_CATEGS = new HashMap<String, String>();

  static {
    BUG_CATEGS.put("STYLE", "Usability");
    BUG_CATEGS.put("NOISE", "Reliability");
    BUG_CATEGS.put("CORRECTNESS", "Reliability");
    BUG_CATEGS.put("SECURITY", "Reliability");
    BUG_CATEGS.put("BAD_PRACTICE", "Maintainability");
    BUG_CATEGS.put("MT_CORRECTNESS", "Reliability");
    BUG_CATEGS.put("PERFORMANCE", "Efficiency");
    BUG_CATEGS.put("I18N", "Portability");
    BUG_CATEGS.put("MALICIOUS_CODE", "Reliability");
  }

  public static void main(String[] args) throws Exception {
    List<FindBugsBug> bugs = getBugsToImport();
    String generatedXML = parseMessages(bugs);
    File out = new File(".", "rules.xml");
    IOUtil.copy(generatedXML.getBytes(), new FileOutputStream(out));
    System.out.println("Written to " + out.getPath());
  }

  private static List<FindBugsBug> getBugsToImport() throws MalformedURLException, IOException, XMLStreamException {
    URL messages = new URL("http://findbugs.googlecode.com/svn/branches/" + FOR_VERSION + "/findbugs/etc/findbugs.xml");
    InputStream in = messages.openStream();
    final List<FindBugsBug> bugs = new ArrayList<FindBugsBug>();
    StaxParser p = new StaxParser(new StaxParser.XmlStreamHandler() {

      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        SMInputCursor bugPatterns = rootCursor.descendantElementCursor("BugPattern");
        collectBugDefs(bugs, bugPatterns);
      }

      private void collectBugDefs(final List<FindBugsBug> bugs, SMInputCursor bugPatterns) throws XMLStreamException {
        while (bugPatterns.getNext() != null) {
          if (bugPatterns.asEvent().isEndElement()) continue;

          String experimental = bugPatterns.getAttrValue("experimental");
          boolean isExperimental = (StringUtils.isNotEmpty(experimental) && Boolean.valueOf(experimental)) || bugPatterns.getAttrValue("category").equals("EXPERIMENTAL");
          String deprecated = bugPatterns.getAttrValue("deprecated");
          boolean isDeprecated = StringUtils.isNotEmpty(deprecated) && Boolean.valueOf(deprecated);
          if (!isExperimental && !isDeprecated) {
            bugs.add(new FindBugsBug(bugPatterns.getAttrValue("category"), bugPatterns.getAttrValue("type")));
          }
        }
      }
    });
    p.parse(in);
    in.close();
    return bugs;
  }

  private static String parseMessages(final List<FindBugsBug> bugs) throws MalformedURLException, IOException, XMLStreamException {
    URL messages = new URL("http://findbugs.googlecode.com/svn/branches/" + FOR_VERSION + "/findbugs/etc/messages.xml");

    InputStream in = messages.openStream();
    final List<Rule> rules = new ArrayList<Rule>();
    StaxParser p = new StaxParser(new StaxParser.XmlStreamHandler() {

      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        Map<String, String> bugCategoriesDecr = new HashMap<String, String>();
        SMInputCursor childrens = rootCursor.childElementCursor();
        while (childrens.getNext() != null) {
          if (childrens.asEvent().isEndElement()) continue;
          if (childrens.getLocalName().equals("BugCategory")) {
            String bugCateg = childrens.getAttrValue("category");
            bugCategoriesDecr.put(bugCateg, childrens.childElementCursor("Description").advance().collectDescendantText());
          } else if (childrens.getLocalName().equals("BugPattern")) {
            String bugType = childrens.getAttrValue("type");
            FindBugsBug bug = getFindBugsBugByType(bugType, bugs);
            if (bug == null) continue;

            rules.add(getRuleForBug(bugType, bug, bugCategoriesDecr, childrens));
          }
        }
      }
    });

    p.parse(in);
    in.close();
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    return parser.toXml(rules);
  }

  private static Rule getRuleForBug(String bugType, FindBugsBug bug,
                                    Map<String, String> bugCategoriesDecr, SMInputCursor childrens) throws XMLStreamException {
    Rule rule = new Rule();
    rule.setKey(bugType);
    rule.setConfigKey(bugType);

    String rulesCateg = BUG_CATEGS.get(bug.getCategory());
    if (StringUtils.isEmpty(rulesCateg)) {
      throw new RuntimeException("Rules cat not found " + bug.getCategory());
    }
    rule.setRulesCategory(new RulesCategory(rulesCateg));

    SMInputCursor descendents = childrens.childElementCursor();
    while (descendents.getNext() != null) {
      if (descendents.asEvent().isStartElement()) {
        if (descendents.getLocalName().equals("ShortDescription")) {
          String categName = bugCategoriesDecr.get(bug.getCategory());
          if (StringUtils.isEmpty(categName)) throw new RuntimeException("Cat not found " + bug.getCategory());
          rule.setName(categName + " - " + descendents.collectDescendantText());
        } else if (descendents.getLocalName().equals("Details")) {
          rule.setDescription(descendents.collectDescendantText());
        }
      }
    }
    return rule;
  }

  private static FindBugsBug getFindBugsBugByType(String type, List<FindBugsBug> bugs) {
    for (FindBugsBug findBugsBug : bugs) {
      if (findBugsBug.getType().equals(type)) {
        return findBugsBug;
      }
    }
    return null;
  }

  private static class FindBugsBug {
    private String category;
    private String type;

    public FindBugsBug(String category, String type) {
      super();
      this.category = category;
      this.type = type;
    }

    public String getCategory() {
      return category;
    }

    public String getType() {
      return type;
    }

  }

}
