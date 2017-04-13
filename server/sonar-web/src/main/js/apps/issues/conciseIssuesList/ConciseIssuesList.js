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
import ConciseIssue from './ConciseIssue';
import { scrollToElement } from '../../../helpers/scrolling';
import type { Issue } from '../../../components/issue/types';

type Props = {|
  issues: Array<Issue>,
  onIssueSelect: (string) => void,
  selected?: string
|};

export default class ConciseIssuesList extends React.PureComponent {
  nodes: { [string]: HTMLElement };
  props: Props;

  constructor(props: Props) {
    super(props);
    this.nodes = {};
  }

  componentDidMount() {
    if (this.props.selected) {
      this.ensureSelectedVisible();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selected && prevProps.selected !== this.props.selected) {
      this.ensureSelectedVisible();
    }
  }

  ensureSelectedVisible() {
    const { selected } = this.props;
    if (selected) {
      const scrollableElement = document.querySelector('.layout-page-side');
      const element = this.nodes[selected];
      if (element && scrollableElement) {
        scrollToElement(element, 150, 100, scrollableElement);
      }
    }
  }

  innerRef = (issue: string) =>
    (node: HTMLElement) => {
      this.nodes[issue] = node;
    };

  render() {
    return (
      <div>
        {this.props.issues.map((issue, index) => (
          <ConciseIssue
            key={issue.key}
            innerRef={this.innerRef(issue.key)}
            issue={issue}
            onSelect={this.props.onIssueSelect}
            previousIssue={index > 0 ? this.props.issues[index - 1] : null}
            selected={issue.key === this.props.selected}
          />
        ))}
      </div>
    );
  }
}
