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
import * as React from 'react';
import { getLocations, getSelectedLocation } from '../utils';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { scrollToElement } from '../../../helpers/scrolling';

interface Props {
  branchLike: T.BranchLike | undefined;
  loadIssues: (component: string, from: number, to: number) => Promise<T.Issue[]>;
  locationsNavigator: boolean;
  onIssueChange: (issue: T.Issue) => void;
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  openIssue: T.Issue;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default class IssuesSourceViewer extends React.PureComponent<Props> {
  node?: HTMLElement | null;

  componentDidUpdate(prevProps: Props) {
    const { openIssue, selectedLocationIndex } = this.props;

    // Scroll back to the issue when the selected location is set to -1
    const shouldScrollBackToIssue =
      selectedLocationIndex === -1 && selectedLocationIndex !== prevProps.selectedLocationIndex;
    if (
      prevProps.openIssue.component === openIssue.component &&
      (prevProps.openIssue !== openIssue || shouldScrollBackToIssue)
    ) {
      this.scrollToIssue();
    }
  }

  scrollToIssue = (smooth = true) => {
    if (this.node) {
      const element = this.node.querySelector(`[data-issue="${this.props.openIssue.key}"]`);
      if (element) {
        this.handleScroll(element, smooth);
      }
    }
  };

  handleScroll = (element: Element, smooth = true) => {
    const offset = window.innerHeight / 2;
    scrollToElement(element, { topOffset: offset - 100, bottomOffset: offset, smooth });
  };

  handleLoaded = () => {
    this.scrollToIssue(false);
  };

  render() {
    const { openIssue, selectedFlowIndex, selectedLocationIndex } = this.props;

    const locations = getLocations(openIssue, selectedFlowIndex);
    const selectedLocation = getSelectedLocation(
      openIssue,
      selectedFlowIndex,
      selectedLocationIndex
    );

    const component = selectedLocation ? selectedLocation.component : openIssue.component;

    // if location is selected, show (and load) code around it
    // otherwise show code around the open issue
    const aroundLine = selectedLocation
      ? selectedLocation.textRange.startLine
      : openIssue.textRange && openIssue.textRange.endLine;

    // replace locations in another file with `undefined` to keep the same location indexes
    const highlightedLocations = locations.map(location =>
      location.component === component ? location : undefined
    );

    const highlightedLocationMessage =
      this.props.locationsNavigator && selectedLocationIndex !== undefined
        ? selectedLocation && { index: selectedLocationIndex, text: selectedLocation.msg }
        : undefined;

    const allMessagesEmpty = locations !== undefined && locations.every(location => !location.msg);

    // do not load issues when open another file for a location
    const loadIssues =
      component === openIssue.component ? this.props.loadIssues : () => Promise.resolve([]);
    const selectedIssue = component === openIssue.component ? openIssue.key : undefined;

    return (
      <div ref={node => (this.node = node)}>
        <SourceViewer
          aroundLine={aroundLine}
          branchLike={this.props.branchLike}
          component={component}
          displayAllIssues={true}
          displayIssueLocationsCount={false}
          displayIssueLocationsLink={false}
          displayLocationMarkers={!allMessagesEmpty}
          highlightedLocationMessage={highlightedLocationMessage}
          highlightedLocations={highlightedLocations}
          loadIssues={loadIssues}
          onIssueChange={this.props.onIssueChange}
          onIssueSelect={this.props.onIssueSelect}
          onLoaded={this.handleLoaded}
          onLocationSelect={this.props.onLocationSelect}
          scroll={this.handleScroll}
          selectedIssue={selectedIssue}
        />
      </div>
    );
  }
}
