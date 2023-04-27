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
import classNames from 'classnames';
import * as React from 'react';
import { ButtonPlain } from '../../../components/controls/buttons';
import { IssueMessageHighlighting } from '../../../components/issue/IssueMessageHighlighting';
import FlowsList from '../../../components/locations/FlowsList';
import LocationsList from '../../../components/locations/LocationsList';
import TypeHelper from '../../../components/shared/TypeHelper';
import { translateWithParameters } from '../../../helpers/l10n';
import { FlowType, Issue } from '../../../types/types';
import { getLocations } from '../utils';
import ConciseIssueLocations from './ConciseIssueLocations';

interface Props {
  issue: Issue;
  onClick: (issueKey: string) => void;
  onFlowSelect: (index?: number) => void;
  onLocationSelect: (index: number) => void;
  selected: boolean;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default function ConciseIssueBox(props: Props) {
  const { issue, selected, selectedFlowIndex, selectedLocationIndex } = props;

  const handleClick = () => {
    props.onClick(issue.key);
  };

  const locations = React.useMemo(
    () => getLocations(issue, selectedFlowIndex),
    [issue, selectedFlowIndex]
  );

  return (
    <div
      className={classNames('concise-issue-box', 'clearfix', { selected })}
      onClick={selected ? undefined : handleClick}
    >
      <ButtonPlain className="concise-issue-box-message" aria-current={selected}>
        <IssueMessageHighlighting
          message={issue.message}
          messageFormattings={issue.messageFormattings}
        />
      </ButtonPlain>
      <div className="concise-issue-box-attributes">
        <TypeHelper className="display-block little-spacer-right" type={issue.type} />
        {issue.flowsWithType.length > 0 ? (
          <span className="concise-issue-box-flow-indicator muted">
            {translateWithParameters(
              'issue.x_data_flows',
              issue.flowsWithType.filter((f) => f.type === FlowType.DATA).length
            )}
          </span>
        ) : (
          <ConciseIssueLocations
            issue={issue}
            onFlowSelect={props.onFlowSelect}
            selectedFlowIndex={selectedFlowIndex}
          />
        )}
      </div>
      {selected &&
        (issue.flowsWithType.length > 0 ? (
          <FlowsList
            flows={issue.flowsWithType}
            onLocationSelect={props.onLocationSelect}
            onFlowSelect={props.onFlowSelect}
            selectedLocationIndex={selectedLocationIndex}
            selectedFlowIndex={selectedFlowIndex}
          />
        ) : (
          <LocationsList
            locations={locations}
            componentKey={issue.component}
            onLocationSelect={props.onLocationSelect}
            selectedLocationIndex={selectedLocationIndex}
          />
        ))}
    </div>
  );
}
