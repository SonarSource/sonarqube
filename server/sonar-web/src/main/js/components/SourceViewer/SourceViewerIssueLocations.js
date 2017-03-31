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
import { DraggableCore } from 'react-draggable';
import classNames from 'classnames';
import { throttle } from 'lodash';
import { scrollToElement } from '../../helpers/scrolling';
import { translate } from '../../helpers/l10n';
import type { Issue, FlowLocation } from '../issue/types';
import type { IndexedIssueLocation } from './helpers/indexing';

type Props = {
  height: number,
  issue: Issue,
  onResize: (height: number) => void,
  onSelectLocation: (flowIndex: number, locationIndex: number) => void,
  selectedLocation: IndexedIssueLocation | null
};

type State = {
  fixed: boolean,
  locationBlink: boolean
};

export default class SourceViewerIssueLocations extends React.Component {
  fixedNode: HTMLElement;
  locations: { [string]: HTMLElement } = {};
  node: HTMLElement;
  props: Props;
  rootNode: HTMLElement;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { fixed: true, locationBlink: false };
    this.handleScroll = throttle(this.handleScroll, 50);
  }

  componentDidMount() {
    this.bindShortcuts();
    this.listenScroll();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.selectedLocation !== this.props.selectedLocation) {
      this.setState({ locationBlink: false });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.selectedLocation !== this.props.selectedLocation &&
      this.props.selectedLocation != null
    ) {
      this.scrollToLocation();
    }
  }

  componentWillUnmount() {
    this.unbindShortcuts();
    this.unlistenScroll();
  }

  bindShortcuts() {
    document.addEventListener('keydown', this.handleKeyPress);
  }

  unbindShortcuts() {
    document.removeEventListener('keydown', this.handleKeyPress);
  }

  listenScroll() {
    window.addEventListener('scroll', this.handleScroll);
  }

  unlistenScroll() {
    window.removeEventListener('scroll', this.handleScroll);
  }

  blinkLocation = () => {
    this.setState({ locationBlink: true });
    setTimeout(() => this.setState({ locationBlink: false }), 1000);
  };

  handleScroll = () => {
    const rootNodeTop = this.rootNode.getBoundingClientRect().top;
    const fixedNodeRect = this.fixedNode.getBoundingClientRect();
    const fixedNodeTop = fixedNodeRect.top;
    const fixedNodeBottom = fixedNodeRect.bottom;
    this.setState((state: State) => {
      if (state.fixed) {
        if (rootNodeTop <= fixedNodeTop) {
          return { fixed: false };
        }
      } else if (fixedNodeBottom >= window.innerHeight) {
        return { fixed: true };
      }
    });
  };

  handleDrag = (e: Event, data: { deltaY: number }) => {
    let height = this.props.height - data.deltaY;
    if (height < 100) {
      height = 100;
    }
    if (height > window.innerHeight / 2) {
      height = window.innerHeight / 2;
    }
    this.props.onResize(height);
  };

  scrollToLocation() {
    const { selectedLocation } = this.props;
    if (selectedLocation != null) {
      const key = `${selectedLocation.flowIndex}-${selectedLocation.locationIndex}`;
      const locationElement = this.locations[key];
      if (locationElement) {
        scrollToElement(locationElement, 15, 15, this.node);
      }
    }
  }

  handleSelectPrev() {
    const { issue, selectedLocation } = this.props;
    if (!selectedLocation) {
      if (issue.flows.length > 0) {
        // move to the first location of the first flow
        this.props.onSelectLocation(0, 0);
      }
    } else {
      const currentFlow = issue.flows[selectedLocation.flowIndex];
      if (
        currentFlow.locations != null &&
        currentFlow.locations.length > selectedLocation.locationIndex + 1
      ) {
        // move to the next location for the same flow
        this.props.onSelectLocation(selectedLocation.flowIndex, selectedLocation.locationIndex + 1);
      } else if (selectedLocation.flowIndex > 0) {
        // move to the first location of the previous flow
        this.props.onSelectLocation(selectedLocation.flowIndex - 1, 0);
      } else {
        this.blinkLocation();
      }
    }
  }

  handleSelectNext() {
    const { issue, selectedLocation } = this.props;
    if (!selectedLocation) {
      if (issue.flows.length > 0) {
        // move to the last location of the first flow
        const firstFlow = issue.flows[0];
        if (firstFlow.locations != null) {
          this.props.onSelectLocation(0, firstFlow.locations.length - 1);
        }
      }
    } else if (selectedLocation.locationIndex > 0) {
      // move to the previous location for the same flow
      this.props.onSelectLocation(selectedLocation.flowIndex, selectedLocation.locationIndex - 1);
    } else if (issue.flows.length > selectedLocation.flowIndex + 1) {
      // move to the last location of the next flow
      const nextFlow = issue.flows[selectedLocation.flowIndex + 1];
      if (nextFlow.locations) {
        this.props.onSelectLocation(selectedLocation.flowIndex + 1, nextFlow.locations.length - 1);
      }
    } else {
      this.blinkLocation();
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
        this.handleSelectNext();
      }

      if (selectPrev) {
        e.preventDefault();
        this.handleSelectPrev();
      }
    }
  };

  reverseLocations(locations: Array<*>) {
    return [...locations].reverse();
  }

  isLocationSelected(flowIndex: number, locationIndex: number) {
    const { selectedLocation } = this.props;
    if (selectedLocation == null) {
      return false;
    } else {
      return selectedLocation.flowIndex === flowIndex &&
        selectedLocation.locationIndex === locationIndex;
    }
  }

  handleLocationClick(flowIndex: number, locationIndex: number, e: SyntheticInputEvent) {
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
    // note that locations order is reversed
    const selected = this.isLocationSelected(flowIndex, locations.length - locationIndex - 1);

    return (
      <li key={key} ref={node => this.locations[key] = node} className="spacer-bottom">
        {line != null && <code className="source-issue-locations-line">L{line}</code>}
        <a
          className={classNames('issue-location-message', 'flash', 'flash-heavy', {
            selected,
            in: selected && this.state.locationBlink
          })}
          href="#"
          onClick={this.handleLocationClick.bind(
            this,
            flowIndex,
            locations.length - locationIndex - 1
          )}>
          {displayIndex && <strong>{locationIndex + 1}: </strong>}
          {location.msg}
        </a>
      </li>
    );
  };

  render() {
    const { flows } = this.props.issue;
    const { height } = this.props;

    const className = classNames('source-issue-locations-panel', { fixed: this.state.fixed });

    return (
      <AutoSizer disableHeight={true}>
        {({ width }) => (
          <div
            ref={node => this.rootNode = node}
            className="source-issue-locations"
            style={{ width, height }}>
            <div
              ref={node => this.fixedNode = node}
              className={className}
              style={{ width, height }}>
              <header className="source-issue-locations-header" />
              <div className="source-issue-locations-shortcuts">
                <span className="shortcut-button">Alt</span>
                {' + '}
                <span className="shortcut-button">↑</span>
                {' '}
                <span className="shortcut-button">↓</span>
                {' '}
                {translate('source_viewer.to_navigate_issue_locations')}
              </div>
              <ul
                ref={node => this.node = node}
                className="source-issue-locations-list"
                style={{ height: height - 15 }}>
                {flows.map(
                  (flow, flowIndex) =>
                    flow.locations != null &&
                    this.reverseLocations(flow.locations).map((location, locationIndex) =>
                      this.renderLocation(location, flowIndex, locationIndex, flow.locations || []))
                )}
              </ul>
              <DraggableCore axis="y" onDrag={this.handleDrag} offsetParent={document.body}>
                <div className="workspace-viewer-resize" />
              </DraggableCore>
            </div>
          </div>
        )}
      </AutoSizer>
    );
  }
}
