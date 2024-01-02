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
import styled from '@emotion/styled';
import { DiscreetLink, themeBorder, themeContrast } from 'design-system';
import React, { PureComponent } from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { collapsePath } from '../../../helpers/path';
import { FlowLocation, Issue } from '../../../types/types';
import IssueLocation from './IssueLocation';

interface Props {
  issue: Pick<Issue, 'key' | 'type'>;
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

const COLLAPSE_PATH_LIMIT = 15;
export const VISIBLE_LOCATIONS_COLLAPSE = 2;

export default class IssueLocationsCrossFile extends PureComponent<Props, State> {
  state: State = { collapsed: true };

  componentDidUpdate(prevProps: Props) {
    if (this.props.issue.key !== prevProps.issue.key) {
      this.setState({ collapsed: true });
    }

    // expand locations list as soon as a location in the middle of the list is selected
    const { locations: nextLocations } = this.props;
    if (
      this.props.selectedLocationIndex &&
      this.props.selectedLocationIndex > 0 &&
      nextLocations !== undefined &&
      this.props.selectedLocationIndex < nextLocations.length - 1
    ) {
      this.setState({ collapsed: false });
    }
  }

  handleMoreLocationsClick = () => {
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

  renderLocation = (index: number, message: string | undefined) => {
    return (
      <IssueLocation
        index={index}
        key={index}
        message={message}
        onClick={this.props.onLocationSelect}
        selected={index === this.props.selectedLocationIndex}
      />
    );
  };

  renderGroup = (
    group: LocationGroup,
    groupIndex: number,
    { onlyFirst = false, onlyLast = false } = {},
  ) => {
    const { firstLocationIndex } = group;
    const lastLocationIndex = group.locations.length - 1;

    return (
      <div key={groupIndex}>
        <ComponentName className="sw-pb-1 sw-body-sm-highlight">
          {collapsePath(group.componentName ?? '', COLLAPSE_PATH_LIMIT)}
        </ComponentName>
        {group.locations.length > 0 && (
          <GroupBody className="sw-ml-2 sw-pl-2">
            {onlyFirst && this.renderLocation(firstLocationIndex, group.locations[0].msg)}

            {onlyLast &&
              this.renderLocation(
                firstLocationIndex + lastLocationIndex,
                group.locations[lastLocationIndex].msg,
              )}

            {!onlyFirst &&
              !onlyLast &&
              group.locations.map((location, index) =>
                this.renderLocation(firstLocationIndex + index, location.msg),
              )}
          </GroupBody>
        )}
      </div>
    );
  };

  render() {
    const { locations } = this.props;
    const groups = this.groupByFile(locations);

    if (
      locations.length > VISIBLE_LOCATIONS_COLLAPSE &&
      groups.length > 1 &&
      this.state.collapsed
    ) {
      const firstGroup = groups[0];
      const lastGroup = groups[groups.length - 1];
      return (
        <div className="sw-flex sw-flex-col sw-gap-4">
          {this.renderGroup(firstGroup, 0, { onlyFirst: true })}
          <div>
            <ExpandLink
              blurAfterClick
              onClick={this.handleMoreLocationsClick}
              preventDefault
              to={{}}
            >
              {translateWithParameters(
                'issues.show_x_more_locations',
                locations.length - VISIBLE_LOCATIONS_COLLAPSE,
              )}
            </ExpandLink>
          </div>
          {this.renderGroup(lastGroup, groups.length - 1, { onlyLast: true })}
        </div>
      );
    }
    return (
      <div className="sw-flex sw-flex-col sw-gap-4">
        {groups.map((group, groupIndex) => this.renderGroup(group, groupIndex))}
      </div>
    );
  }
}

const GroupBody = styled.div`
  border-left: ${themeBorder('default', 'subnavigationExecutionFlowBorder')};
`;

const ComponentName = styled.div`
  color: ${themeContrast('subnavigation')};
`;

const ExpandLink = styled(DiscreetLink)`
  color: ${themeContrast('subnavigationSubheading')};
`;
