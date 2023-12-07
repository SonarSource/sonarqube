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
import { DiscreetLink, ExecutionFlowAccordion, SubnavigationFlowSeparator } from 'design-system';
import React, { Fragment, useCallback, useRef } from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Flow, FlowType, Issue } from '../../../types/types';
import { getLocations, getTypedFlows } from '../utils';
import IssueLocations from './IssueLocations';
import IssueLocationsNavigatorKeyboardHint from './IssueLocationsNavigatorKeyboardHint';

interface Props {
  issue: Pick<
    Issue,
    'component' | 'key' | 'flows' | 'secondaryLocations' | 'type' | 'flowsWithType'
  >;
  onFlowSelect: (index: number | undefined) => void;
  onLocationSelect: (index: number) => void;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function IssueLocationsNavigator(props: Props) {
  const { issue, onFlowSelect, onLocationSelect, selectedFlowIndex, selectedLocationIndex } = props;
  const accordionElement = useRef<HTMLDivElement | null>(null);
  const hasFlows =
    issue.flows.some((f) => f.length > 0) ||
    issue.flowsWithType.some((f) => f.locations.length > 0);
  const hasOneFlow = issue.flows.length === 1;
  const hasSecondaryLocations = issue.secondaryLocations.length > 0;

  const handleAccordionClick = useCallback(
    (index: number | undefined) => {
      if (onFlowSelect) {
        onFlowSelect(index === selectedFlowIndex ? undefined : index);
        if (index !== selectedFlowIndex) {
          accordionElement.current?.scrollIntoView({
            block: 'center',
            behavior: 'smooth',
          });
        }
      }
    },
    [selectedFlowIndex, onFlowSelect],
  );

  if (!hasSecondaryLocations && !hasFlows) {
    return null;
  }

  if (hasSecondaryLocations || hasOneFlow) {
    const locations = getLocations(issue, selectedFlowIndex);

    if (locations.every((location) => !location.msg)) {
      return null;
    }

    return (
      <>
        <SubnavigationFlowSeparator className="sw-my-2" />
        <div className="sw-flex sw-flex-col sw-gap-1">
          <IssueLocations
            concealed={hasSecondaryLocations}
            issue={issue}
            locations={locations}
            onLocationSelect={onLocationSelect}
            selectedLocationIndex={selectedLocationIndex}
          />
        </div>
        {locations.length > 1 && <IssueLocationsNavigatorKeyboardHint showLeftRightHint={false} />}
      </>
    );
  }

  const hasFlowsWithType = issue.flowsWithType.length > 0;
  const flows = hasFlowsWithType
    ? getTypedFlows(issue.flowsWithType)
    : issue.flows.map((locations) => ({ type: FlowType.EXECUTION, locations }));

  if (flows.length > 0) {
    return (
      <>
        <div className="sw-flex sw-flex-col sw-gap-4 sw-mt-4">
          {flows.map((flow, index) => (
            <Fragment key={`${issue.key}-flow-${index}`}>
              <ExecutionFlowAccordion
                expanded={index === selectedFlowIndex}
                header={
                  <span>
                    <strong>
                      {flow.locations.length > 1
                        ? translateWithParameters('issue.flow.x_steps', flow.locations.length)
                        : translate('issue.flow.1_step')}
                    </strong>{' '}
                    {getExecutionFlowLabel(flow, hasFlowsWithType)}
                  </span>
                }
                hidden={
                  index !== selectedFlowIndex &&
                  flow.type === FlowType.EXECUTION &&
                  hasFlowsWithType
                }
                id={`${issue.key}-flow-${index}`}
                innerRef={(n) => (accordionElement.current = n)}
                onClick={() => {
                  handleAccordionClick(index);
                }}
              >
                <IssueLocations
                  issue={issue}
                  locations={flow.locations}
                  onLocationSelect={onLocationSelect}
                  selectedLocationIndex={selectedLocationIndex}
                />
              </ExecutionFlowAccordion>
              {index !== selectedFlowIndex &&
                flow.type === FlowType.EXECUTION &&
                hasFlowsWithType && (
                  <div>
                    <DiscreetLink
                      onClick={() => {
                        handleAccordionClick(index);
                      }}
                      preventDefault
                      to="{{}}"
                    >
                      {translateWithParameters(
                        'issue.show_full_execution_flow',
                        flow.locations.length,
                      )}
                    </DiscreetLink>
                  </div>
                )}
            </Fragment>
          ))}
        </div>
        <IssueLocationsNavigatorKeyboardHint showLeftRightHint />
      </>
    );
  }

  return null;
}

function getExecutionFlowLabel(flow: Flow, hasFlowsWithType: boolean) {
  if (hasFlowsWithType && flow.type !== FlowType.EXECUTION) {
    return flow.description;
  }

  return translate('issues.execution_flow');
}
