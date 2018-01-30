package org.sonar.server.webhook.ws;

import com.google.common.io.Resources;
import java.util.ArrayList;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Webhooks;

import static org.sonar.server.webhook.ws.WebhooksWsParameters.ORGANIZATION_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.PROJECT_KEY_PARAM;
import static org.sonar.server.webhook.ws.WebhooksWsParameters.SEARCH_ACTION;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements WebhooksWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public SearchAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {

    WebService.NewAction action = controller.createAction(SEARCH_ACTION)
      .setDescription("Search for webhooks associated to an organization or a project.<br/>")
      .setSince("7.1")
      .setResponseExample(Resources.getResource(this.getClass(), "example-webhooks-search.json"))
      .setHandler(this);

    action.createParam(ORGANIZATION_KEY_PARAM)
      .setDescription("Organization key. If no organization is provided, the default organization is used.")
      .setInternal(true)
      .setRequired(false)
      .setExampleValue(KEY_ORG_EXAMPLE_001);

    action.createParam(PROJECT_KEY_PARAM)
      .setDescription("Project key")
      .setRequired(false)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    Webhooks.SearchWsResponse.Builder searchResponse = Webhooks.SearchWsResponse.newBuilder();

    // FIXME : hard coded to test plumbing
    ArrayList<WebhookSearchDTO> webhookSearchDTOS = new ArrayList<>();
    webhookSearchDTOS.add(new WebhookSearchDTO("UUID-1", "my first webhook", "http://www.my-webhook-listener.com/sonarqube"));
    webhookSearchDTOS.add(new WebhookSearchDTO("UUID-2", "my 2nd webhook", "https://www.my-other-webhook-listener.com/fancy-listner"));

    for (WebhookSearchDTO dto : webhookSearchDTOS) {
      searchResponse.addWebhooksBuilder()
        .setKey(dto.getKey())
        .setName(dto.getName())
        .setUrl(dto.getUrl());
    }

    writeProtobuf(searchResponse.build(), request, response);
  }

}

// {"key":"UUID-1","name":,"url":,"latestDelivery":{"id":"d1","at":"2017-07-14T04:40:00+0200","success":true,"httpStatus":200,"durationMs":10}},
// {"key":"UUID-2","name":"my 2nd
// webhook","url":"https://www.my-other-webhook-listener.com/fancy-listner","latestDelivery":{"id":"d2","at":"2017-07-14T04:40:00+0200","success":true,"httpStatus":200,"durationMs":10}}
