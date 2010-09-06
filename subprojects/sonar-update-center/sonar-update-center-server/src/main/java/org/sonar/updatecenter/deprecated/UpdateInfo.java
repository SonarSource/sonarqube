package org.sonar.updatecenter.deprecated;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.Arrays;
import java.util.List;

/**
 * Information about updates.
 *
 * @author Evgeny Mandrikov
 */
@XStreamAlias("updateInfo")
public class UpdateInfo {

  public Sonar sonar;

  @XStreamImplicit(itemFieldName = "plugin")
  public List<Plugin> plugins;

  public UpdateInfo(Sonar sonar, List<Plugin> plugins) {
    this.sonar = sonar;
    this.plugins = plugins;
  }

  public static void main(String[] args) {
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);

    Plugin plugin = new Plugin("sonar-test-plugin");
    plugin.setVersion("0.1");
    plugin.setName("Sonar Test Plugin");
    plugin.setDescription("Test");
    plugin.setHomepage("http://homepage");
    plugin.setDownloadUrl("http://download");
    plugin.setRequiredSonarVersion("2.0");

    System.out.println(xstream.toXML(new UpdateInfo(new Sonar("2.0"), Arrays.asList(plugin))));
  }
}
