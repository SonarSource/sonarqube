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
import { sortBy } from 'lodash';
import * as React from 'react';
import { ButtonLink } from '../../../components/controls/buttons';
import ChevronDownIcon from '../../../components/icons/ChevronDownIcon';
import { translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component } from '../../../types/types';
import QualityGateCondition from './QualityGateCondition';

const LEVEL_ORDER = ['ERROR', 'WARN'];

export interface QualityGateConditionsProps {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  collapsible?: boolean;
  failedConditions: QualityGateStatusConditionEnhanced[];
}

const MAX_CONDITIONS = 5;

export function QualityGateConditions(props: QualityGateConditionsProps) {
  const { branchLike, collapsible, component, failedConditions } = props;
  const [collapsed, toggleCollapsed] = React.useState(Boolean(collapsible));

  const handleToggleCollapsed = React.useCallback(() => toggleCollapsed(!collapsed), [collapsed]);

  const sortedConditions = sortBy(failedConditions, (condition) =>
    LEVEL_ORDER.indexOf(condition.level)
  );

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
    <ul
      className="overview-quality-gate-conditions-list"
      id="overview-quality-gate-conditions-list"
    >
      {renderConditions.map((condition) => (
        <QualityGateCondition
          branchLike={branchLike}
          component={component}
          condition={condition}
          key={condition.measure.metric.key}
        />
      ))}
      {renderCollapsed && (
        <li>
          <ButtonLink
            className="overview-quality-gate-conditions-list-collapse"
            onClick={handleToggleCollapsed}
          >
            {translateWithParameters(
              'overview.X_more_failed_conditions',
              sortedConditions.length - MAX_CONDITIONS
            )}
            <ChevronDownIcon className="little-spacer-left" />
          </ButtonLink>
        </li>
      )}
    </ul>
  );
}

export default React.memo(QualityGateConditions);
