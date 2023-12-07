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
import { ExecutionFlowIcon } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { FlowType, Issue } from '../../../types/types';

interface Props {
  issue: Pick<Issue, 'flows' | 'flowsWithType' | 'secondaryLocations'>;
}

export default function IssueItemLocationsQuantity(props: Props) {
  const { quantity, message } = getLocationsText(props.issue);

  if (message) {
    return (
      <div className="sw-flex sw-items-center sw-justify-center sw-gap-1 sw-overflow-hidden">
        <ExecutionFlowIcon />
        <span className="sw-truncate" title={`${quantity} ${message}`}>
          <span className="sw-body-sm-highlight">{quantity}</span> {message}
        </span>
      </div>
    );
  }

  return null;
}

function getLocationsText(issue: Props['issue']) {
  const { flows, flowsWithType, secondaryLocations } = issue;
  if (flows.length === 1 || flowsWithType.length === 1) {
    return { quantity: 1, message: translate('issues.execution_flow') };
  } else if (flows.length > 1) {
    return { quantity: flows.length, message: translate('issues.execution_flows') };
  } else if (flowsWithType.length > 1) {
    const dataFlows = flowsWithType.filter(({ type }) => type === FlowType.DATA);
    return {
      quantity: dataFlows.length,
      message: translate(dataFlows.length > 1 ? 'issues.data_flows' : 'issues.data_flow'),
    };
  } else if (secondaryLocations.length === 1) {
    return { quantity: secondaryLocations.length, message: translate('issues.location') };
  } else if (secondaryLocations.length > 1) {
    return { quantity: secondaryLocations.length, message: translate('issues.locations') };
  }

  return {};
}
