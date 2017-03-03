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
import AutoSizer from 'react-virtualized/dist/commonjs/AutoSizer';
import classNames from 'classnames';
import { scrollToElement } from '../../helpers/scrolling';
import type { Issue, FlowLocation } from '../issue/types';
import type { IndexedIssueLocation } from './helpers/indexing';

type Props = {
  issue: Issue,
  onSelectLocation: (flowIndex: number, locationIndex: number) => void,
  selectedLocation: IndexedIssueLocation | null
};

export default class SourceViewerIssueLocations extends React.Component {
  locations: { [string]: HTMLElement } = {};
  node: HTMLElement;
  props: Props;

  componentDidMount () {
    this.bindShortcuts();
  }

  componentDidUpdate (prevProps: Props) {
    if (prevProps.selectedLocation !== this.props.selectedLocation && this.props.selectedLocation != null) {
      this.scrollToLocation();
    }
  }

  componentWillUnmount () {
    this.unbindShortcuts();
  }

  bindShortcuts () {
    document.addEventListener('keydown', this.handleKeyPress);
  }

  unbindShortcuts () {
    document.removeEventListener('keydown', this.handleKeyPress);
  }

  scrollToLocation () {
    const { selectedLocation } = this.props;
    if (selectedLocation != null) {
      const key = `${selectedLocation.flowIndex}-${selectedLocation.locationIndex}`;
      const locationElement = this.locations[key];
      if (locationElement) {
        scrollToElement(locationElement, 15, 15, this.node);
      }
    }
  }

  handleSelectNext () {
    const { issue, selectedLocation } = this.props;
    if (!selectedLocation) {
      if (issue.flows.length > 0) {
        // move to the first location of the first flow
        this.props.onSelectLocation(0, 0);
      }
    } else {
      const currentFlow = issue.flows[selectedLocation.flowIndex];
      if (currentFlow.locations != null) {
        if (currentFlow.locations.length > selectedLocation.locationIndex + 1) {
          // move to the next location for the same flow
          this.props.onSelectLocation(selectedLocation.flowIndex, selectedLocation.locationIndex + 1);
        } else if (issue.flows.length > selectedLocation.flowIndex + 1) {
          // move to the first location of the next flow
          this.props.onSelectLocation(selectedLocation.flowIndex + 1, 0);
        }
      }
    }
  }

  handleSelectPrev () {
    const { issue, selectedLocation } = this.props;
    if (!selectedLocation) {
      if (issue.flows.length > 0) {
        // move to the last location of the last flow
        const lastFlow = issue.flows[issue.flows.length - 1];
        if (lastFlow.locations != null) {
          this.props.onSelectLocation(issue.flows.length - 1, lastFlow.locations.length - 1);
        }
      }
    } else if (selectedLocation.locationIndex > 0) {
      // move to the previous location for the same flow
      this.props.onSelectLocation(selectedLocation.flowIndex, selectedLocation.locationIndex - 1);
    } else if (selectedLocation.flowIndex > 0) {
      // move to the last location of the previous flow
      const prevFlow = issue.flows[selectedLocation.flowIndex - 1];
      if (prevFlow.locations) {
        this.props.onSelectLocation(selectedLocation.flowIndex - 1, prevFlow.locations.length - 1);
      }
    }
  }

  handleKeyPress = (e: Object) => {
    const tagName = e.target.tagName.toUpperCase();
    const shouldHandle = tagName !== 'INPUT' && tagName !== 'TEXTAREA' && tagName !== 'BUTTON';

    if (shouldHandle) {
      const selectNext = e.keyCode === 40 && e.altKey;
      const selectPrev = e.keyCode === 38 && e.altKey;

      if (selectNext) {
        e.preventDefault();
        // keep reversed order
        this.handleSelectPrev();
      }

      if (selectPrev) {
        e.preventDefault();
        // keep reversed order
        this.handleSelectNext();
      }
    }
  };

  reverseLocations (locations: Array<*>) {
    return [...locations].reverse();
  }

  isLocationSelected (flowIndex: number, locationIndex: number) {
    const { selectedLocation } = this.props;
    if (selectedLocation == null) {
      return false;
    } else {
      return selectedLocation.flowIndex === flowIndex && selectedLocation.locationIndex === locationIndex;
    }
  }

  handleLocationClick (flowIndex: number, locationIndex: number, e: SyntheticInputEvent) {
    e.preventDefault();
    this.props.onSelectLocation(flowIndex, locationIndex);
  }

  renderLocation = (
    location: FlowLocation,
    flowIndex: number,
    locationIndex: number,
    locations: Array<*>
  ) => {
    const displayIndex = locations.length > 1;

    const line = location.textRange ? location.textRange.startLine : null;

    const key = `${flowIndex}-${locationIndex}`;

    return (
      <li key={key} ref={node => this.locations[key] = node} className="spacer-bottom">
        {line != null && (
          <code className="source-issue-locations-line">L{line}</code>
        )}

        <a className={classNames('issue-location-message', {
          // note that locations order is reversed
          'selected': this.isLocationSelected(flowIndex, locations.length - locationIndex - 1)
        })}
          href="#"
          onClick={this.handleLocationClick.bind(this, flowIndex, locations.length - locationIndex - 1)}>
          {displayIndex && <strong>{locationIndex + 1}: </strong>}
          {location.msg}
        </a>
      </li>
    );
  };

  render () {
    const { flows } = this.props.issue;

    return (
      <div className="source-issue-locations">
        <AutoSizer disableHeight={true}>
          {({ width }) => (
            <div className="source-issue-locations-fixed" style={{ width }}>
              <header className="source-issue-locations-header"/>
              <div className="source-issue-locations-shortcuts">
                <span className="shortcut-button">Alt</span>
                {' + '}
                <span className="shortcut-button">&uarr;</span>
                {' '}
                <span className="shortcut-button">&darr;</span>
                {' '}
                to quicky navigate issue locations
              </div>
              <ul ref={node => this.node = node} className="source-issue-locations-list">
                {flows.map((flow, flowIndex) => (
                  flow.locations != null && this.reverseLocations(flow.locations).map((location, locationIndex) => (
                    this.renderLocation(location, flowIndex, locationIndex, flow.locations || [])
                  ))
                ))}
              </ul>
            </div>
          )}
          </AutoSizer>
      </div>
    );
  }
}
