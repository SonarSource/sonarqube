package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Plugin;
import org.sonar.wsclient.services.WSUtils;

/**
 * @since 2.4
 */
public class PluginUnmarshaller extends AbstractUnmarshaller<Plugin> {

  @Override
  protected Plugin parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    return new Plugin()
      .setKey(utils.getString(json, "key"))
      .setName(utils.getString(json, "name"))
      .setVersion(utils.getString(json, "version"));
  }

}
