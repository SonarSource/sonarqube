package org.sonar.server.v2.api.hotspot.controller;

import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.hotspot.response.ExpireHotspotsRestResponse;
import org.sonar.server.v2.api.hotspot.service.HotspotService;

public class DefaultHotspotController implements HotspotController {

    private final UserSession userSession;
    private final HotspotService hotspotService;

    public DefaultHotspotController(UserSession userSession, HotspotService hotspotService) {
        this.userSession = userSession;
        this.hotspotService = hotspotService;
    }

    @Override
    public ExpireHotspotsRestResponse expireHotspots() {
        userSession.checkIsSystemAdministrator();

        int expiredCount = hotspotService.expireHotspotExceptions();

        return new ExpireHotspotsRestResponse(expiredCount);
    }
}