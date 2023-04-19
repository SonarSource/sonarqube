/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { BasicSeparator, Link } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
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
    <ul id="overview-quality-gate-conditions-list" className="sw-mb-2">
      {renderConditions.map((condition) => (
        <div key={condition.measure.metric.key}>
          <QualityGateCondition
            branchLike={branchLike}
            component={component}
            condition={condition}
          />
          <BasicSeparator />
        </div>
      ))}
      {renderCollapsed && (
        <li className="sw-flex sw-justify-center sw-my-3">
          <Link onClick={handleToggleCollapsed} to={{}} preventDefault={true}>
            <span className="sw-font-semibold sw-text-sm">{translate('show_more')}</span>
          </Link>
        </li>
      )}
    </ul>
  );
}

export default React.memo(QualityGateConditions);
