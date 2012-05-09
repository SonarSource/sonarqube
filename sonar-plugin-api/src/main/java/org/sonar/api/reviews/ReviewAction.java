package org.sonar.api.reviews;

import com.google.common.annotations.Beta;
import org.sonar.api.ServerExtension;

import java.util.Map;

/**
 * @since 3.1
 */
@Beta
public interface ReviewAction extends ServerExtension {

  String getId();

  String getName();

  void execute(Map<String, String> reviewContext);

}
