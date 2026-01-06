package org.sonar.server.v2.api.hotspot.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueChangePostProcessor;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.user.UserSession;

@ServerSide
public class HotspotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotspotService.class);
    private static final int BATCH_SIZE = 500;

    private final DbClient dbClient;
    private final UserSession userSession;
    private final TransitionService transitionService;
    private final WebIssueStorage issueStorage;
    private final IssueChangePostProcessor issueChangePostProcessor;

    public HotspotService(DbClient dbClient, UserSession userSession, TransitionService transitionService,
            WebIssueStorage issueStorage, IssueChangePostProcessor issueChangePostProcessor) {
        this.dbClient = dbClient;
        this.userSession = userSession;
        this.transitionService = transitionService;
        this.issueStorage = issueStorage;
        this.issueChangePostProcessor = issueChangePostProcessor;
    }

    /**
     * Expires all security hotspot exceptions that have reached their expiry date. Automatically transitions expired
     * hotspots from EXCEPTION status back to TO_REVIEW status.
     *
     * @return the number of hotspots that were expired
     */
    public int expireHotspotExceptions() {
        long currentTime = System.currentTimeMillis();

        try (DbSession dbSession = dbClient.openSession(false)) {
            // Find all expired hotspot keys
            List<String> expiredKeys = dbClient.issueDao()
                    .selectExpiredHotspotKeys(dbSession, currentTime);

            if (expiredKeys.isEmpty()) {
                return 0;
            }

            LOGGER.info("Found {} expired hotspot keys to process", expiredKeys.size());

            // Process in batches to avoid long-running transactions
            int totalExpired = 0;
            for (int i = 0; i < expiredKeys.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, expiredKeys.size());
                List<String> batchKeys = expiredKeys.subList(i, endIndex);
                totalExpired += processBatch(dbSession, batchKeys);
            }

            dbSession.commit();
            LOGGER.info("Successfully expired {} hotspots", totalExpired);
            return totalExpired;
        }
    }

    private int processBatch(DbSession dbSession, List<String> hotspotKeys) {
        // Batch load all hotspots
        List<IssueDto> hotspots = dbClient.issueDao().selectByKeys(dbSession, hotspotKeys);
        if (hotspots.isEmpty()) {
            return 0;
        }

        // Batch load components
        Set<String> componentUuids = hotspots.stream().map(IssueDto::getComponentUuid).collect(Collectors.toSet());
        List<ComponentDto> components = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
        Map<String, ComponentDto> componentsByUuid = components.stream()
                .collect(Collectors.toMap(ComponentDto::uuid, c -> c));

        // Process transitions and collect modified issues
        IssueChangeContext context = IssueChangeContext.newBuilder().setDate(new Date(System.currentTimeMillis()))
                .setUserUuid(userSession.getUuid()).withRefreshMeasures().build();
        String transitionKey = DefaultTransitions.RESET_AS_TO_REVIEW;
        List<DefaultIssue> modifiedIssues = new ArrayList<>();
        List<String> successfullyTransitionedKeys = new ArrayList<>();
        Set<String> touchedComponentUuids = new java.util.HashSet<>();

        for (IssueDto hotspot : hotspots) {
            try {
                DefaultIssue defaultIssue = hotspot.toDefaultIssue();

                if (transitionService.doTransition(defaultIssue, context, transitionKey)) {
                    modifiedIssues.add(defaultIssue);
                    successfullyTransitionedKeys.add(hotspot.getKey());
                    touchedComponentUuids.add(defaultIssue.componentUuid());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to transition hotspot {}: {}", hotspot.getKey(), e.getMessage(), e);
                // Continue processing other hotspots
            }
        }

        if (modifiedIssues.isEmpty()) {
            return 0;
        }

        // Bulk clear expiry dates
        dbClient.issueDao().bulkClearHotspotExceptionExpiryDate(dbSession, successfullyTransitionedKeys);

        // Batch save all modified issues (this batches indexing)
        issueStorage.save(dbSession, modifiedIssues);

        // Batch refresh measures
        List<ComponentDto> touchedComponents = touchedComponentUuids.stream().map(componentsByUuid::get)
                .filter(java.util.Objects::nonNull).toList();
        if (!touchedComponents.isEmpty()) {
            issueChangePostProcessor.process(dbSession, modifiedIssues, touchedComponents, false);
        }

        return successfullyTransitionedKeys.size();
    }
}