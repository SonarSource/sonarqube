#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resourcetab.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.services.Resource;

public class SampleResourceTab extends Page {

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    return new Label("This is a sample");
  }
}