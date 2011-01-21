package org.sonar.api.resources;

import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.utils.SonarException;

/**
 * @since 2.6
 */
public final class DuplicatedSourceException extends SonarException {

  public DuplicatedSourceException(Resource resource) {
    super(ObjectUtils.toString(resource));
  }
}
