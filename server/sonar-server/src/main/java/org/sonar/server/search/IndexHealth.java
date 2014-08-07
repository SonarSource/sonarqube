package org.sonar.server.search;

import java.util.Date;

public class IndexHealth {
  private static final int SEGMENTS_THRESHOLD = 5;
  private static final double PENDING_DELETION_THRESHOLD = 0.08D;

  String name;
  long documentCount;
  Date lastSync;
  long segmentCount;
  long pendingDeletion;

  public String getName() {
    return name;
  }

  public long getDocumentCount() {
    return documentCount;
  }

  public Date getLastSynchronization() {
    return lastSync;
  }

  public boolean isOptimized() {
    return segmentCount < SEGMENTS_THRESHOLD && pendingDeletion < documentCount * PENDING_DELETION_THRESHOLD;
  }

  public long getSegmentcount() {
    return segmentCount;
  }

  public long getPendingDeletion() {
    return pendingDeletion;
  }
}
