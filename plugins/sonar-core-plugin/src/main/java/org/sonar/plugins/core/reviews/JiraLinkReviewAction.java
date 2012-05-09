package org.sonar.plugins.core.reviews;

import org.sonar.api.reviews.LinkReviewAction;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @since 3.1
 */
public class JiraLinkReviewAction implements LinkReviewAction {

  public String getId() {
    return "jira-link";
  }

  public String getName() {
    return "Link to JIRA";
  }

  public void execute(Map<String, String> reviewContext) {
    System.out.println("============>");

    for (Entry<String, String> mapEntry : reviewContext.entrySet()) {
      System.out.println(mapEntry.getKey() + " // " + mapEntry.getValue());
    }

    System.out.println("<============");
  }

}
