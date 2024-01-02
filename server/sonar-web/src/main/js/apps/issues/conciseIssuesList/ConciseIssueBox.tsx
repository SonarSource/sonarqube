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
  scroll: (element: Element) => void;
  selected: boolean;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default class ConciseIssueBox extends React.PureComponent<Props> {
  messageElement?: HTMLElement | null;

  componentDidMount() {
    if (this.props.selected) {
      this.handleScroll();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selected && prevProps.selected !== this.props.selected) {
      this.handleScroll();
    }
  }

  handleClick = () => {
    this.props.onClick(this.props.issue.key);
  };

  handleScroll = () => {
    if (this.messageElement) {
      this.props.scroll(this.messageElement);
    }
  };

  render() {
    const { issue, selected, selectedFlowIndex, selectedLocationIndex } = this.props;

    const locations = getLocations(issue, selectedFlowIndex);

    return (
      <div
        className={classNames('concise-issue-box', 'clearfix', { selected })}
        onClick={selected ? undefined : this.handleClick}
      >
        <ButtonPlain
          className="concise-issue-box-message"
          aria-current={selected}
          innerRef={(node) => (this.messageElement = node)}
          onClick={this.handleClick}
        >
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
              onFlowSelect={this.props.onFlowSelect}
              selectedFlowIndex={selectedFlowIndex}
            />
          )}
        </div>
        {selected &&
          (issue.flowsWithType.length > 0 ? (
            <FlowsList
              flows={issue.flowsWithType}
              onLocationSelect={this.props.onLocationSelect}
              onFlowSelect={this.props.onFlowSelect}
              selectedLocationIndex={selectedLocationIndex}
              selectedFlowIndex={selectedFlowIndex}
            />
          ) : (
            <LocationsList
              locations={locations}
              componentKey={issue.component}
              onLocationSelect={this.props.onLocationSelect}
              selectedLocationIndex={selectedLocationIndex}
            />
          ))}
      </div>
    );
  }
}
