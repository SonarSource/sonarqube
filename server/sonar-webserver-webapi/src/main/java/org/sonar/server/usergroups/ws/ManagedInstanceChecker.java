package org.sonar.server.usergroups.ws;

import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;

public class ManagedInstanceChecker {

  private final ManagedInstanceService managedInstanceService;

  public ManagedInstanceChecker(ManagedInstanceService managedInstanceService) {
    this.managedInstanceService = managedInstanceService;
  }

  public void checkInstanceIsNotExternallyManaged() {
    BadRequestException.checkRequest(!managedInstanceService.isInstanceExternallyManaged(), "Operation not allowed when instance is externally managed.");
  }
}
