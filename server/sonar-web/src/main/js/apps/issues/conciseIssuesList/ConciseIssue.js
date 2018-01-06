/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ConciseIssueBox from './ConciseIssueBox';
import ConciseIssueComponent from './ConciseIssueComponent';
/*:: import type { Issue } from '../../../components/issue/types'; */

/*::
type Props = {|
  issue: Issue,
  onFlowSelect: number => void,
  onLocationSelect: number => void,
  onSelect: string => void,
  previousIssue: ?Issue,
  scroll: HTMLElement => void,
  selected: boolean,
  selectedFlowIndex: ?number,
  selectedLocationIndex: ?number
|};
*/

export default class ConciseIssue extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { issue, previousIssue, selected } = this.props;

    const displayComponent = previousIssue == null || previousIssue.component !== issue.component;

    return (
      <div>
        {displayComponent && <ConciseIssueComponent path={issue.componentLongName} />}
        <ConciseIssueBox
          issue={issue}
          onClick={this.props.onSelect}
          onFlowSelect={this.props.onFlowSelect}
          onLocationSelect={this.props.onLocationSelect}
          scroll={this.props.scroll}
          selected={selected}
          selectedFlowIndex={selected ? this.props.selectedFlowIndex : null}
          selectedLocationIndex={selected ? this.props.selectedLocationIndex : null}
        />
      </div>
    );
  }
}
