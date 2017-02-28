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
import type { Issue, FlowLocation } from '../issue/types';

export default class SourceViewerIssueLocations extends React.Component {
  props: {
    issue: Issue,
    onSelectLocation: (flow: number, location: number) => void,
    selectedLocation: { flow: number, location: number } | null
  };

  componentDidMount () {
    this.bindShortcuts();
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

  handleSelectNext () {
    const { issue, selectedLocation } = this.props;
    if (!selectedLocation) {
      if (issue.flows.length > 0) {
        // move to the first location of the first flow
        this.props.onSelectLocation(0, 0);
      }
    } else {
      const currentFlow = issue.flows[selectedLocation.flow];
      if (currentFlow.locations != null) {
        if (currentFlow.locations.length > selectedLocation.location + 1) {
          // move to the next location for the same flow
          this.props.onSelectLocation(selectedLocation.flow, selectedLocation.location + 1);
        } else if (issue.flows.length > selectedLocation.flow + 1) {
          // move to the first location of the next flow
          this.props.onSelectLocation(selectedLocation.flow + 1, 0);
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
    } else if (selectedLocation.location > 0) {
      // move to the previous location for the same flow
      this.props.onSelectLocation(selectedLocation.flow, selectedLocation.location - 1);
    } else if (selectedLocation.flow > 0) {
      // move to the last location of the previous flow
      const prevFlow = issue.flows[selectedLocation.flow - 1];
      if (prevFlow.locations) {
        this.props.onSelectLocation(selectedLocation.flow - 1, prevFlow.locations.length - 1);
      }
    }
  }

  handleKeyPress = (e: Object) => {
    const tagName = e.target.tagName.toUpperCase();
    const shouldHandle = tagName !== 'INPUT' && tagName !== 'TEXTAREA' && tagName !== 'BUTTON';

    /* eslint-disable no-console */
    console.log('bam!');

    if (shouldHandle) {
      const selectNext = e.keyCode === 40 && e.altKey;
      const selectPrev = e.keyCode === 38 && e.altKey;

      if (selectNext) {
        e.preventDefault();
        this.handleSelectNext();
      }

      if (selectPrev) {
        e.preventDefault();
        this.handleSelectPrev();
      }
    }
  };

  isLocationSelected (flowIndex: number, locationIndex: number) {
    const { selectedLocation } = this.props;
    if (selectedLocation == null) {
      return false;
    } else {
      return selectedLocation.flow === flowIndex && selectedLocation.location === locationIndex;
    }
  }

  renderLocation = (
    location: FlowLocation,
    flowIndex: number,
    locationIndex: number,
    locations?: Array<*>
  ) => {
    const displayIndex = locations != null && locations.length > 1;

    const line = location.textRange ? location.textRange.startLine : null;

    return (
      <li key={`${flowIndex}-${locationIndex}`} className="spacer-bottom">
        {line != null && (
          <code className="source-issue-locations-line">L{line}</code>
        )}

        <span className={classNames('issue-location-message', {
          'selected': this.isLocationSelected(flowIndex, locationIndex)
        })}>
          {displayIndex && <strong>{locationIndex}: </strong>}
          {location.msg}
        </span>
      </li>
    );
  };

  render () {
    return (
      <div className="source-issue-locations">
        <AutoSizer>
          {({ width, height }) => (
            <div className="source-issue-locations-fixed" style={{ width, height }}>
              <header className="source-issue-locations-header"/>
              <ul className="source-issue-locations-list">
                {this.props.issue.flows.map((flow, flowIndex) => (
                  flow.locations != null && flow.locations.map((location, locationIndex) => (
                    this.renderLocation(location, flowIndex, locationIndex, flow.locations)
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
