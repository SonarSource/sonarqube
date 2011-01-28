package itests;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

public final class ExcludedResourceFilter implements ResourceFilter {

  public boolean isIgnored(Resource resource) {
    return Scopes.isFile(resource) && StringUtils.contains(resource.getName(), "ExcludedByFilter");
  }
}
