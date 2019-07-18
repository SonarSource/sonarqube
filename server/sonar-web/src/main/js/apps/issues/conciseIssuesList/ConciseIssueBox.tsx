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
import * as classNames from 'classnames';
import * as React from 'react';
import TypeHelper from '../../../components/shared/TypeHelper';
import LocationNavigationKeyboardShortcuts from '../components/LocationNavigationKeyboardShortcuts';
import ConciseIssueLocations from './ConciseIssueLocations';
import ConciseIssueLocationsNavigator from './ConciseIssueLocationsNavigator';

interface Props {
  issue: T.Issue;
  onClick: (issueKey: string) => void;
  onFlowSelect: (index: number) => void;
  onLocationSelect: (index: number) => void;
  scroll: (element: Element, bottomOffset?: number) => void;
  selected: boolean;
  selectedFlowIndex: number | undefined;
  selectedLocationIndex: number | undefined;
}

export default class ConciseIssueBox extends React.PureComponent<Props> {
  messageElement?: HTMLElement | null;
  rootElement?: HTMLElement | null;

  componentDidMount() {
    if (this.props.selected) {
      this.handleScroll();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selected && prevProps.selected !== this.props.selected) {
      this.handleScroll();
    }
  }

  handleClick = (event: React.MouseEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClick(this.props.issue.key);
  };

  handleScroll = () => {
    const { selectedFlowIndex } = this.props;
    const { flows, secondaryLocations } = this.props.issue;

    const locations = flows.length > 0 ? flows[selectedFlowIndex || 0] : secondaryLocations;

    if (!locations || locations.length < 15) {
      // if there are no locations, or there are just few
      // then ensuse that the whole box is visible
      if (this.rootElement) {
        this.props.scroll(this.rootElement);
      }
    } else if (this.messageElement) {
      // otherwise scroll until the the message element is located on top
      this.props.scroll(this.messageElement, window.innerHeight - 250);
    }
  };

  render() {
    const { issue, selected } = this.props;

    const clickAttributesMain = selected
      ? {}
      : { onClick: this.handleClick, role: 'listitem', tabIndex: 0 };

    const clickAttributesTitle = selected
      ? { onClick: this.handleClick, role: 'listitem', tabIndex: 0 }
      : {};

    return (
      <div
        className={classNames('concise-issue-box', 'clearfix', { selected })}
        ref={node => (this.rootElement = node)}
        {...clickAttributesMain}>
        <div
          className="concise-issue-box-message"
          {...clickAttributesTitle}
          ref={node => (this.messageElement = node)}>
          {issue.message}
        </div>
        <div className="concise-issue-box-attributes">
          <TypeHelper className="display-block little-spacer-right" type={issue.type} />
          <ConciseIssueLocations
            issue={issue}
            onFlowSelect={this.props.onFlowSelect}
            selectedFlowIndex={this.props.selectedFlowIndex}
          />
        </div>
        {selected && (
          <>
            <ConciseIssueLocationsNavigator
              issue={issue}
              onLocationSelect={this.props.onLocationSelect}
              scroll={this.props.scroll}
              selectedFlowIndex={this.props.selectedFlowIndex}
              selectedLocationIndex={this.props.selectedLocationIndex}
            />
            <LocationNavigationKeyboardShortcuts issue={issue} />
          </>
        )}
      </div>
    );
  }
}
