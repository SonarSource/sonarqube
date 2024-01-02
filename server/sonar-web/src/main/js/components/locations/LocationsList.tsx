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
import { uniq } from 'lodash';
import * as React from 'react';
import { FlowLocation } from '../../types/types';
import CrossFileLocationNavigator from './CrossFileLocationNavigator';
import SingleFileLocationNavigator from './SingleFileLocationNavigator';

interface Props {
  componentKey: string;
  locations: FlowLocation[];
  onLocationSelect: (index: number) => void;
  selectedLocationIndex?: number;
  showCrossFile?: boolean;
}

export default class LocationsList extends React.PureComponent<Props> {
  render() {
    const { locations, componentKey, selectedLocationIndex, showCrossFile = true } = this.props;

    const locationComponents = [componentKey, ...locations.map((location) => location.component)];
    const isCrossFile = uniq(locationComponents).length > 1;

    if (!locations || locations.length === 0 || locations.every((location) => !location.msg)) {
      return null;
    }

    if (isCrossFile && showCrossFile) {
      return (
        <CrossFileLocationNavigator
          locations={locations}
          onLocationSelect={this.props.onLocationSelect}
          selectedLocationIndex={selectedLocationIndex}
        />
      );
    }
    return (
      <ul className="spacer-top">
        {locations.map((location, index) => (
          // eslint-disable-next-line react/no-array-index-key
          <li className="display-flex-column" key={index}>
            <SingleFileLocationNavigator
              index={index}
              message={location.msg}
              messageFormattings={location.msgFormattings}
              onClick={this.props.onLocationSelect}
              selected={index === selectedLocationIndex}
            />
          </li>
        ))}
      </ul>
    );
  }
}
