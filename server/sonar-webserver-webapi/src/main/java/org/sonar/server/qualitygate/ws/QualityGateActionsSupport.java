/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualitygate.ws;

import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceEntitlement;
import org.sonarqube.ws.Qualitygates;

public class QualityGateActionsSupport {
  private final QualityGatesWsSupport wsSupport;
  private final AiCodeAssuranceEntitlement aiCodeAssuranceEntitlement;

  public QualityGateActionsSupport(QualityGatesWsSupport wsSupport, AiCodeAssuranceEntitlement aiCodeAssuranceEntitlement) {
    this.wsSupport = wsSupport;
    this.aiCodeAssuranceEntitlement = aiCodeAssuranceEntitlement;
  }

  Qualitygates.Actions getActions(DbSession dbSession, QualityGateDto qualityGate, @Nullable QualityGateDto defaultQualityGate) {
    boolean isDefault = defaultQualityGate != null && Objects.equals(defaultQualityGate.getUuid(), qualityGate.getUuid());
    boolean isBuiltIn = qualityGate.isBuiltIn();
    boolean isQualityGateAdmin = wsSupport.isQualityGateAdmin();
    boolean canLimitedEdit = isQualityGateAdmin || wsSupport.hasLimitedPermission(dbSession, qualityGate);
    return Qualitygates.Actions.newBuilder()
      .setCopy(isQualityGateAdmin)
      .setRename(!isBuiltIn && isQualityGateAdmin)
      .setManageConditions(!isBuiltIn && canLimitedEdit)
      .setDelete(!isDefault && !isBuiltIn && isQualityGateAdmin)
      .setSetAsDefault(!isDefault && isQualityGateAdmin)
      .setAssociateProjects(!isDefault && isQualityGateAdmin)
      .setDelegate(!isBuiltIn && canLimitedEdit)
      .setManageAiCodeAssurance(aiCodeAssuranceEntitlement.isEnabled() && !isBuiltIn && isQualityGateAdmin)
      .build();
  }

  boolean isAiCodeAssuranceEnabled() {
    return aiCodeAssuranceEntitlement.isEnabled();
  }
}
