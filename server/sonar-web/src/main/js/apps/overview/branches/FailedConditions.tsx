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
import { CardSeparator, TextError } from 'design-system';
import _ from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { isDiffMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, QualityGate } from '../../../types/types';
import ZeroNewIssuesSimplificationGuide from '../components/ZeroNewIssuesSimplificationGuide';
import QualityGateConditions from './QualityGateConditions';

export interface FailedConditionsProps {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  failedConditions: QualityGateStatusConditionEnhanced[];
  isApplication?: boolean;
  isNewCode: boolean;
  qualityGate?: QualityGate;
}

export default function FailedConditions({
  isApplication,
  isNewCode,
  qualityGate,
  failedConditions,
  component,
  branchLike,
}: FailedConditionsProps) {
  const [newCodeFailedConditions, overallFailedConditions] = _.partition(
    failedConditions,
    (condition) => isDiffMetric(condition.metric),
  );

  return (
    <>
      {!isApplication && (
        <>
          <TextError
            className="sw-mb-3"
            text={
              <FormattedMessage
                id="quality_gates.conditions.x_conditions_failed"
                values={{
                  conditions: isNewCode
                    ? newCodeFailedConditions.length
                    : overallFailedConditions.length,
                }}
              />
            }
          />
          <CardSeparator />
        </>
      )}
      {qualityGate?.isBuiltIn && isNewCode && (
        <ZeroNewIssuesSimplificationGuide qualityGate={qualityGate} />
      )}
      <QualityGateConditions
        component={component}
        branchLike={branchLike}
        failedConditions={isNewCode ? newCodeFailedConditions : overallFailedConditions}
        isBuiltInQualityGate={isNewCode && qualityGate?.isBuiltIn}
      />
    </>
  );
}
