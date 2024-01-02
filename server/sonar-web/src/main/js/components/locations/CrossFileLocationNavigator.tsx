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
import * as React from 'react';
import { translateWithParameters } from '../../helpers/l10n';
import { collapsePath } from '../../helpers/path';
import { MessageFormatting } from '../../types/issues';
import { FlowLocation } from '../../types/types';
import './CrossFileLocationNavigator.css';
import SingleFileLocationNavigator from './SingleFileLocationNavigator';

interface Props {
  locations: FlowLocation[];
  onLocationSelect: (index: number) => void;
  selectedLocationIndex: number | undefined;
}

interface State {
  collapsed: boolean;
}

interface LocationGroup {
  component: string | undefined;
  componentName: string | undefined;
  firstLocationIndex: number;
  locations: FlowLocation[];
}

const MAX_PATH_LENGTH = 15;

export default class CrossFileLocationNavigator extends React.PureComponent<Props, State> {
  state: State = { collapsed: true };

  componentDidUpdate() {
    const { locations, selectedLocationIndex } = this.props;
    if (
      selectedLocationIndex &&
      selectedLocationIndex > 0 &&
      locations !== undefined &&
      selectedLocationIndex < locations.length - 1 &&
      this.state.collapsed
    ) {
      this.setState({ collapsed: false });
    }
  }

  handleMoreLocationsClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ collapsed: false });
  };

  groupByFile = (locations: FlowLocation[]) => {
    const groups: LocationGroup[] = [];

    let currentLocations: FlowLocation[] = [];
    let currentComponent: string | undefined;
    let currentComponentName: string | undefined;
    let currentFirstLocationIndex = 0;

    for (let index = 0; index < locations.length; index++) {
      const location = locations[index];
      if (location.component === currentComponent) {
        currentLocations.push(location);
      } else {
        if (currentLocations.length > 0) {
          groups.push({
            component: currentComponent,
            componentName: currentComponentName,
            firstLocationIndex: currentFirstLocationIndex,
            locations: currentLocations,
          });
        }
        currentLocations = [location];
        currentComponent = location.component;
        currentComponentName = location.componentName;
        currentFirstLocationIndex = index;
      }
    }

    if (currentLocations.length > 0) {
      groups.push({
        component: currentComponent,
        componentName: currentComponentName,
        firstLocationIndex: currentFirstLocationIndex,
        locations: currentLocations,
      });
    }

    return groups;
  };

  renderLocation = (
    index: number,
    message: string | undefined,
    messageFormattings: MessageFormatting[] | undefined
  ) => {
    return (
      <SingleFileLocationNavigator
        index={index}
        key={index}
        message={message}
        messageFormattings={messageFormattings}
        onClick={this.props.onLocationSelect}
        selected={index === this.props.selectedLocationIndex}
      />
    );
  };

  renderGroup = (
    group: LocationGroup,
    groupIndex: number,
    { onlyFirst = false, onlyLast = false } = {}
  ) => {
    const { firstLocationIndex } = group;
    const lastLocationIndex = group.locations.length - 1;
    return (
      <div className="locations-navigator-file" key={groupIndex}>
        <div className="location-file">
          <i className="location-file-circle little-spacer-right" />
          {collapsePath(group.componentName || '', MAX_PATH_LENGTH)}
        </div>
        {group.locations.length > 0 && (
          <div className="location-file-locations">
            {onlyFirst &&
              this.renderLocation(
                firstLocationIndex,
                group.locations[0].msg,
                group.locations[0].msgFormattings
              )}

            {onlyLast &&
              this.renderLocation(
                firstLocationIndex + lastLocationIndex,
                group.locations[lastLocationIndex].msg,
                group.locations[lastLocationIndex].msgFormattings
              )}

            {!onlyFirst &&
              !onlyLast &&
              group.locations.map((location, index) =>
                this.renderLocation(
                  firstLocationIndex + index,
                  location.msg,
                  location.msgFormattings
                )
              )}
          </div>
        )}
      </div>
    );
  };

  render() {
    const { locations } = this.props;
    const groups = this.groupByFile(locations);
    // below: fold the location list when there are >3 locations
    const MIN_LOCATION_LENGTH = 3;

    if (locations.length > MIN_LOCATION_LENGTH && groups.length > 1 && this.state.collapsed) {
      // the top and bottom locations are always displayed
      const nbLocationsAlwaysDisplayed = 2;

      const firstGroup = groups[0];
      const lastGroup = groups[groups.length - 1];

      return (
        <div className="spacer-top">
          {this.renderGroup(firstGroup, 0, { onlyFirst: true })}
          <div className="locations-navigator-file">
            <div className="location-file">
              <i className="location-file-circle-multiple little-spacer-right" />
              <a className="location-file-more" href="#" onClick={this.handleMoreLocationsClick}>
                {translateWithParameters(
                  'issues.x_more_locations',
                  locations.length - nbLocationsAlwaysDisplayed
                )}
              </a>
            </div>
          </div>
          {this.renderGroup(lastGroup, groups.length - 1, { onlyLast: true })}
        </div>
      );
    }
    return (
      <div className="spacer-top">
        {groups.map((group, groupIndex) => this.renderGroup(group, groupIndex))}
      </div>
    );
  }
}
