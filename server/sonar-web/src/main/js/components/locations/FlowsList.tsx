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
import 'FlowsList.css';
import { uniq } from 'lodash';
import * as React from 'react';
import ConciseIssueLocationBadge from '../../apps/issues/conciseIssuesList/ConciseIssueLocationBadge';
import { translate } from '../../helpers/l10n';
import { Flow, FlowType } from '../../types/types';
import BoxedGroupAccordion from '../controls/BoxedGroupAccordion';
import CrossFileLocationNavigator from './CrossFileLocationNavigator';
import SingleFileLocationNavigator from './SingleFileLocationNavigator';

const FLOW_ORDER_MAP = {
  [FlowType.DATA]: 0,
  [FlowType.EXECUTION]: 1,
};
export interface Props {
  flows: Flow[];
  selectedLocationIndex?: number;
  selectedFlowIndex?: number;
  onFlowSelect: (index?: number) => void;
  onLocationSelect: (index: number) => void;
}

export default function FlowsList(props: Props) {
  const { flows, selectedLocationIndex, selectedFlowIndex } = props;

  flows.sort((f1, f2) => FLOW_ORDER_MAP[f1.type] - FLOW_ORDER_MAP[f2.type]);

  return (
    <div className="issue-flows little-padded-top" role="list">
      {flows.map((flow, index) => {
        const open = selectedFlowIndex === index;

        const locationComponents = flow.locations.map((location) => location.component);
        const isCrossFile = uniq(locationComponents).length > 1;

        let fileLocationNavigator;

        if (isCrossFile) {
          fileLocationNavigator = (
            <CrossFileLocationNavigator
              locations={flow.locations}
              onLocationSelect={props.onLocationSelect}
              selectedLocationIndex={selectedLocationIndex}
            />
          );
        } else {
          fileLocationNavigator = (
            <ul>
              {flow.locations.map((location, locIndex) => (
                // eslint-disable-next-line react/no-array-index-key
                <li className="display-flex-column" key={locIndex}>
                  <SingleFileLocationNavigator
                    index={locIndex}
                    message={location.msg}
                    messageFormattings={location.msgFormattings}
                    onClick={props.onLocationSelect}
                    selected={locIndex === selectedLocationIndex}
                  />
                </li>
              ))}
            </ul>
          );
        }

        return (
          <BoxedGroupAccordion
            className="spacer-top"
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            onClick={() => props.onFlowSelect(open ? undefined : index)}
            open={open}
            noBorder={flow.type === FlowType.EXECUTION}
            title={
              flow.type === FlowType.EXECUTION
                ? translate('issue.execution_flow')
                : flow.description
            }
            renderHeader={() => (
              <ConciseIssueLocationBadge
                count={flow.locations.length}
                flow={true}
                selected={open}
              />
            )}
          >
            {fileLocationNavigator}
          </BoxedGroupAccordion>
        );
      })}
    </div>
  );
}
