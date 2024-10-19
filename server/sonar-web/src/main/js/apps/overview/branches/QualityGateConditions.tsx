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
import { CardSeparator, Link } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component } from '../../../types/types';
import { CAYC_CONDITION_ORDER_PRIORITIES } from '../../quality-gates/utils';
import QualityGateCondition from './QualityGateCondition';
import QualityGateSimplifiedCondition from './QualityGateSimplifiedCondition';

const LEVEL_ORDER = ['ERROR', 'WARN'];

export interface QualityGateConditionsProps {
  branchLike?: BranchLike;
  collapsible?: boolean;
  component: Pick<Component, 'key'>;
  failedConditions: QualityGateStatusConditionEnhanced[];
  isBuiltInQualityGate?: boolean;
  grc: boolean;
}

const MAX_CONDITIONS = 5;

export function QualityGateConditions(props: Readonly<QualityGateConditionsProps>) {
  const { grc, branchLike, collapsible, component, failedConditions, isBuiltInQualityGate } = props;
  const [collapsed, toggleCollapsed] = React.useState(Boolean(collapsible));

  const handleToggleCollapsed = React.useCallback(() => toggleCollapsed(!collapsed), [collapsed]);

  const isSimplifiedCondition = React.useCallback(
    (condition: QualityGateStatusConditionEnhanced) => {
      const { metric } = condition.measure;
      return metric.key === MetricKey.new_violations && isBuiltInQualityGate;
    },
    [isBuiltInQualityGate],
  );

  const sortedConditions = sortBy(failedConditions, [
    (condition) => CAYC_CONDITION_ORDER_PRIORITIES[condition.metric],
    (condition) => LEVEL_ORDER.indexOf(condition.level),
  ]);

  let renderConditions;
  let renderCollapsed;

  if (collapsed && sortedConditions.length > MAX_CONDITIONS) {
    renderConditions = sortedConditions.slice(0, MAX_CONDITIONS);
    renderCollapsed = true;
  } else {
    renderConditions = sortedConditions;
    renderCollapsed = false;
  }

  return (
    <ul id="overview-quality-gate-conditions-list">
      {renderConditions.map((condition, idx) => (
        <div key={condition.measure.metric.key}>
          {isSimplifiedCondition(condition) ? (
            <QualityGateSimplifiedCondition
              branchLike={branchLike}
              component={component}
              condition={condition}
            />
          ) : (
            <QualityGateCondition
              branchLike={branchLike}
              component={component}
              condition={condition}
            />
          )}
          {idx !== renderConditions.length - 1 && <CardSeparator />}
        </div>
      ))}
      {renderCollapsed && (
        <li className="sw-flex sw-justify-center sw-my-3">
          <Link onClick={handleToggleCollapsed} to={{}} preventDefault>
            <span className="sw-font-semibold sw-text-sm">{translate('show_more')}</span>
          </Link>
        </li>
      )}
    </ul>
  );
}

export default React.memo(QualityGateConditions);
