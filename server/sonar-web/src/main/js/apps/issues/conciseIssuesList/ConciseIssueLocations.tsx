/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import ConciseIssueLocationBadge from './ConciseIssueLocationBadge';

interface Props {
  issue: Pick<T.Issue, 'flows' | 'secondaryLocations'>;
  onFlowSelect: (index: number) => void;
  selectedFlowIndex: number | undefined;
}

interface State {
  collapsed: boolean;
}

const LIMIT = 3;

export default class ConciseIssueLocations extends React.PureComponent<Props, State> {
  state: State = { collapsed: true };

  handleExpandClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ collapsed: false });
  };

  renderExpandButton() {
    return (
      <a className="little-spacer-left link-no-underline" href="#" onClick={this.handleExpandClick}>
        ...
      </a>
    );
  }

  render() {
    const { secondaryLocations, flows } = this.props.issue;

    const badges: JSX.Element[] = [];

    if (secondaryLocations.length > 0) {
      badges.push(
        <ConciseIssueLocationBadge
          count={secondaryLocations.length}
          key="-1"
          selected={!this.props.selectedFlowIndex}
        />
      );
    }

    flows.forEach((flow, index) => {
      badges.push(
        <ConciseIssueLocationBadge
          count={flow.length}
          key={index}
          onClick={() => this.props.onFlowSelect(index)}
          selected={index === this.props.selectedFlowIndex}
        />
      );
    });

    return this.state.collapsed ? (
      <div className="concise-issue-locations pull-right">
        {badges.slice(0, LIMIT)}
        {badges.length > LIMIT && this.renderExpandButton()}
      </div>
    ) : (
      <div className="concise-issue-locations spacer-top">{badges}</div>
    );
  }
}
