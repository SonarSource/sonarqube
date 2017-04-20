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
import ConciseIssueLocationBadge from './ConciseIssueLocationBadge';
import type { FlowLocation } from '../../../components/issue/types';

type Props = {|
  flows: Array<{
    locations?: Array<FlowLocation>
  }>
|};

export default class ConciseIssueLocations extends React.PureComponent {
  props: Props;

  render() {
    const { flows } = this.props;

    const secondaryLocations = flows.filter(
      flow => flow.locations != null && flow.locations.length === 1
    ).length;

    const realFlows = flows.filter(flow => flow.locations != null && flow.locations.length > 1);

    return (
      <div className="pull-right">
        {secondaryLocations > 0 && <ConciseIssueLocationBadge count={secondaryLocations} />}

        {realFlows.map((flow, index) => (
          <ConciseIssueLocationBadge
            // $FlowFixMe locations are not null
            count={flow.locations.length}
            key={index}
          />
        ))}
      </div>
    );
  }
}
