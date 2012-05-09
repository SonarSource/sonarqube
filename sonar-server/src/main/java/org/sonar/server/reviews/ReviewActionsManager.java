package org.sonar.server.reviews;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.reviews.ReviewAction;
import org.sonar.api.utils.SonarException;

import java.util.Collection;
import java.util.Map;

/**
 * @since 3.1
 */
public class ReviewActionsManager {

  private Map<String, ReviewAction> idToAction = Maps.newHashMap();
  private Map<String, Collection<ReviewAction>> interfaceToAction = Maps.newHashMap();

  public ReviewActionsManager(ReviewAction[] reviewActions) {
    for (ReviewAction reviewAction : reviewActions) {
      idToAction.put(reviewAction.getId(), reviewAction);
    }
  }

  public ReviewActionsManager() {
    this(new ReviewAction[0]);
  }

  public ReviewAction getAction(String actionId) {
    return idToAction.get(actionId);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<ReviewAction> getActions(String interfaceName) {
    Collection<ReviewAction> result = interfaceToAction.get(interfaceName);
    if (result == null) {
      result = Lists.newArrayList();
      interfaceToAction.put(interfaceName, result);
      try {
        Class interfaceClass = Class.forName(interfaceName);
        for (ReviewAction reviewAction : idToAction.values()) {
          if (interfaceClass.isAssignableFrom(reviewAction.getClass())) {
            result.add(reviewAction);
          }
        }
      } catch (ClassNotFoundException e) {
        throw new SonarException("The following interface for review actions does not exist: " + interfaceName);
      }

    }
    return result;
  }

}
