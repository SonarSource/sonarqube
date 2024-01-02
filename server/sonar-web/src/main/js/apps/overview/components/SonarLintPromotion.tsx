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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import SonarLintIcon from '../../../components/icons/SonarLintIcon';
import { translate } from '../../../helpers/l10n';
import { MetricKey } from '../../../types/metrics';
import { QualityGateStatusCondition } from '../../../types/quality-gates';
import { CurrentUser } from '../../../types/users';

export interface SonarLintPromotionProps {
  currentUser: CurrentUser;
  qgConditions?: QualityGateStatusCondition[];
}

const CONDITIONS_TO_SHOW = [
  MetricKey.new_blocker_violations,
  MetricKey.new_critical_violations,
  MetricKey.new_info_violations,
  MetricKey.new_violations,
  MetricKey.new_major_violations,
  MetricKey.new_minor_violations,
  MetricKey.new_code_smells,
  MetricKey.new_bugs,
  MetricKey.new_vulnerabilities,
  MetricKey.new_security_rating,
  MetricKey.new_maintainability_rating,
  MetricKey.new_reliability_rating,
];

export function SonarLintPromotion({ currentUser, qgConditions }: SonarLintPromotionProps) {
  const showMessage = qgConditions?.some(
    (qgCondition) =>
      CONDITIONS_TO_SHOW.includes(qgCondition.metric as MetricKey) && qgCondition.level === 'ERROR'
  );
  if (!showMessage || currentUser.usingSonarLintConnectedMode) {
    return null;
  }
  return (
    <div className="it__overview__sonarlint-promotion big-spacer-top overview-quality-gate-sonar-lint-info">
      <FormattedMessage
        id="overview.fix_failed_conditions_with_sonarlint"
        defaultMessage={translate('overview.fix_failed_conditions_with_sonarlint')}
        values={{
          link: (
            <>
              <a
                href="https://www.sonarqube.org/sonarlint/?referrer=sonarqube"
                rel="noopener noreferrer"
                target="_blank"
              >
                SonarLint
              </a>
              <SonarLintIcon size={16} />
            </>
          ),
        }}
      />
    </div>
  );
}

export default withCurrentUserContext(SonarLintPromotion);
