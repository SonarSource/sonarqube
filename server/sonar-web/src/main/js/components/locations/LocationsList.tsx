/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { FlowLocation } from '../../types/types';
import CrossFileLocationNavigator from './CrossFileLocationNavigator';
import SingleFileLocationNavigator from './SingleFileLocationNavigator';

interface Props {
  isCrossFile: boolean;
  uniqueKey: string;
  locations: FlowLocation[];
  onLocationSelect: (index: number) => void;
  scroll: (element: Element) => void;
  selectedLocationIndex?: number;
}

export default class LocationsList extends React.PureComponent<Props> {
  render() {
    const { isCrossFile, locations, uniqueKey, selectedLocationIndex } = this.props;

    if (!locations || locations.length === 0 || locations.every(location => !location.msg)) {
      return null;
    }

    if (isCrossFile) {
      return (
        <CrossFileLocationNavigator
          uniqueKey={uniqueKey}
          locations={locations}
          onLocationSelect={this.props.onLocationSelect}
          scroll={this.props.scroll}
          selectedLocationIndex={selectedLocationIndex}
        />
      );
    }
    return (
      <div className="spacer-top">
        {locations.map((location, index) => (
          <SingleFileLocationNavigator
            index={index}
            // eslint-disable-next-line react/no-array-index-key
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
