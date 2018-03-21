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
import * as React from 'react';
import ConciseIssueLocationsNavigatorLocation from './ConciseIssueLocationsNavigatorLocation';
import { Issue } from '../../../app/types';

interface Props {
  issue: Issue;
  onLocationSelect: (index: number) => void;
  scroll: (element: Element) => void;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default class ConciseIssueLocationsNavigator extends React.PureComponent<Props> {
  render() {
    const { selectedFlowIndex, selectedLocationIndex } = this.props;
    const { flows, secondaryLocations } = this.props.issue;

    const locations =
      selectedFlowIndex !== undefined
        ? flows[selectedFlowIndex]
        : flows.length > 0 ? flows[0] : secondaryLocations;

    if (!locations || locations.length === 0 || locations.every(location => !location.msg)) {
      return null;
    }

    return (
      <div className="spacer-top">
        {locations.map((location, index) => (
          <ConciseIssueLocationsNavigatorLocation
            index={index}
            key={index}
            message={location.msg}
            onClick={this.props.onLocationSelect}
            scroll={this.props.scroll}
            selected={index === selectedLocationIndex}
          />
        ))}
      </div>
    );
  }
}
