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
import { BranchLike, Issue } from '../../../app/types';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { scrollToElement } from '../../../helpers/scrolling';

interface Props {
  branchLike: BranchLike | undefined;
  loadIssues: (component: string, from: number, to: number) => Promise<Issue[]>;
  onIssueChange: (issue: Issue) => void;
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  openIssue: Issue;
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

    const locations =
      selectedFlowIndex !== undefined
        ? openIssue.flows[selectedFlowIndex]
        : openIssue.flows.length > 0 ? openIssue.flows[0] : openIssue.secondaryLocations;

    let locationMessage = undefined;
    let locationLine = undefined;

    // We don't want to display a location message when selected location is -1
    if (
      locations !== undefined &&
      selectedLocationIndex !== undefined &&
      selectedLocationIndex >= 0 &&
      locations.length >= selectedLocationIndex
    ) {
      locationMessage = {
        index: selectedLocationIndex,
        text: locations[selectedLocationIndex].msg
      };
      locationLine = locations[selectedLocationIndex].textRange.startLine;
    }

    // if location is selected, show (and load) code around it
    // otherwise show code around the open issue
    const aroundLine = locationLine || (openIssue.textRange && openIssue.textRange.endLine);

    const allMessagesEmpty = locations !== undefined && locations.every(location => !location.msg);

    return (
      <div ref={node => (this.node = node)}>
        <SourceViewer
          aroundLine={aroundLine}
          branchLike={this.props.branchLike}
          component={openIssue.component}
          displayAllIssues={true}
          displayIssueLocationsCount={false}
          displayIssueLocationsLink={false}
          displayLocationMarkers={!allMessagesEmpty}
          highlightedLocationMessage={locationMessage}
          highlightedLocations={locations}
          loadIssues={this.props.loadIssues}
          onIssueChange={this.props.onIssueChange}
          onIssueSelect={this.props.onIssueSelect}
          onLoaded={this.handleLoaded}
          onLocationSelect={this.props.onLocationSelect}
          scroll={this.handleScroll}
          selectedIssue={openIssue.key}
        />
      </div>
    );
  }
}
