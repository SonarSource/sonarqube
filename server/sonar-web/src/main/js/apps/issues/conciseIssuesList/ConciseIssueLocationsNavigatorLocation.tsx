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
import LocationIndex from '../../../components/common/LocationIndex';
import LocationMessage from '../../../components/common/LocationMessage';

interface Props {
  index: number;
  isTaintAnalysis: boolean;
  message: string | undefined;
  onClick: (index: number) => void;
  scroll: (element: Element) => void;
  selected: boolean;
  totalCount: number;
}

export default class ConciseIssueLocationsNavigatorLocation extends React.PureComponent<Props> {
  node?: HTMLElement | null;

  componentDidMount() {
    if (this.props.selected && this.node) {
      this.props.scroll(this.node);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selected && prevProps.selected !== this.props.selected && this.node) {
      this.props.scroll(this.node);
    }
  }

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClick(this.props.index);
  };

  prefixMessage(index: number, message = '', totalCount: number) {
    switch (index) {
      case 0:
        return 'source: ' + message;
      case totalCount - 1:
        return 'sink: ' + message;
      default:
        return message;
    }
  }

  render() {
    const { index, isTaintAnalysis, message, selected, totalCount } = this.props;

    return (
      <div className="little-spacer-top" ref={node => (this.node = node)}>
        <a
          className="concise-issue-locations-navigator-location"
          href="#"
          onClick={this.handleClick}>
          <LocationIndex selected={selected}>{index + 1}</LocationIndex>
          <LocationMessage selected={selected}>
            {isTaintAnalysis ? this.prefixMessage(index, message, totalCount) : message}
          </LocationMessage>
        </a>
      </div>
    );
  }
}
