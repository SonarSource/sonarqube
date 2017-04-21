/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import ConciseIssueLocations from './ConciseIssueLocations';
import ConciseIssueLocationsNavigator from './ConciseIssueLocationsNavigator';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import TypeHelper from '../../../components/shared/TypeHelper';
import type { Issue } from '../../../components/issue/types';

type Props = {|
  issue: Issue,
  onClick: string => void,
  onFlowSelect: number => void,
  onLocationSelect: number => void,
  scroll: HTMLElement => void,
  selected: boolean,
  selectedFlowIndex: ?number,
  selectedLocationIndex: ?number
|};

export default class ConciseIssueBox extends React.PureComponent {
  node: HTMLElement;
  props: Props;

  componentDidMount() {
    // scroll to the message element and not to the root element,
    // because the root element can be huge and exceed the window height
    if (this.props.selected) {
      this.props.scroll(this.node);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selected && prevProps.selected !== this.props.selected) {
      this.props.scroll(this.node);
    }
  }

  handleClick = (event: Event) => {
    event.preventDefault();
    this.props.onClick(this.props.issue.key);
  };

  render() {
    const { issue, selected } = this.props;

    const clickAttributes = selected
      ? {}
      : { onClick: this.handleClick, role: 'listitem', tabIndex: 0 };

    return (
      <div
        className={classNames('concise-issue-box', 'clearfix', { selected })}
        {...clickAttributes}>
        <div className="concise-issue-box-message" ref={node => (this.node = node)}>
          {issue.message}
        </div>
        <div className="concise-issue-box-attributes">
          <TypeHelper type={issue.type} />
          <SeverityHelper className="big-spacer-left" severity={issue.severity} />
          <ConciseIssueLocations
            issue={issue}
            onFlowSelect={this.props.onFlowSelect}
            selectedFlowIndex={this.props.selectedFlowIndex}
          />
        </div>
        {selected &&
          <ConciseIssueLocationsNavigator
            issue={issue}
            onLocationSelect={this.props.onLocationSelect}
            scroll={this.props.scroll}
            selectedFlowIndex={this.props.selectedFlowIndex}
            selectedLocationIndex={this.props.selectedLocationIndex}
          />}
      </div>
    );
  }
}
