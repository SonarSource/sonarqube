package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONObject;
import org.sonar.wsclient.services.Plugin;

/**
 * @since 2.4
 */
public class PluginUnmarshaller extends AbstractUnmarshaller<Plugin> {

  @Override
  protected Plugin parse(JSONObject json) {
    return new Plugin()
      .setKey(JsonUtils.getString(json, "key"))
      .setName(JsonUtils.getString(json, "name"))
      .setVersion(JsonUtils.getString(json, "version"));
  }

}
