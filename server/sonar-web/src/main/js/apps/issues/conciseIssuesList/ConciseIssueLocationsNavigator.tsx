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
import { uniq } from 'lodash';
import * as React from 'react';
import { getLocations } from '../utils';
import ConciseIssueLocationsNavigatorLocation from './ConciseIssueLocationsNavigatorLocation';
import CrossFileLocationsNavigator from './CrossFileLocationsNavigator';

interface Props {
  issue: Pick<T.Issue, 'component' | 'key' | 'flows' | 'secondaryLocations' | 'type'>;
  onLocationSelect: (index: number) => void;
  scroll: (element: Element) => void;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default class ConciseIssueLocationsNavigator extends React.PureComponent<Props> {
  render() {
    const locations = getLocations(this.props.issue, this.props.selectedFlowIndex);

    if (!locations || locations.length === 0 || locations.every(location => !location.msg)) {
      return null;
    }

    const isTaintAnalysis =
      this.props.issue.type === 'VULNERABILITY' && this.props.issue.flows.length > 0;

    const locationComponents = [
      this.props.issue.component,
      ...locations.map(location => location.component)
    ];
    const isCrossFile = uniq(locationComponents).length > 1;

    if (isCrossFile) {
      return (
        <CrossFileLocationsNavigator
          isTaintAnalysis={isTaintAnalysis}
          issue={this.props.issue}
          locations={locations}
          onLocationSelect={this.props.onLocationSelect}
          scroll={this.props.scroll}
          selectedLocationIndex={this.props.selectedLocationIndex}
        />
      );
    } else {
      return (
        <div className="concise-issue-locations-navigator spacer-top">
          {locations.map((location, index) => (
            <ConciseIssueLocationsNavigatorLocation
              index={index}
              isTaintAnalysis={isTaintAnalysis}
              key={index}
              message={location.msg}
              onClick={this.props.onLocationSelect}
              scroll={this.props.scroll}
              selected={index === this.props.selectedLocationIndex}
              totalCount={locations.length}
            />
          ))}
        </div>
      );
    }
  }
}
