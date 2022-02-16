/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import LocationIndex from '../common/LocationIndex';
import LocationMessage from '../common/LocationMessage';
import './SingleFileLocationNavigator.css';

interface Props {
  index: number;
  message: string | undefined;
  onClick: (index: number) => void;
  scroll: (element: Element) => void;
  selected: boolean;
}

export default class SingleFileLocationNavigator extends React.PureComponent<Props> {
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

  render() {
    const { index, message, selected } = this.props;

    return (
      <div className="little-spacer-top" ref={node => (this.node = node)}>
        <a className="locations-navigator" href="#" onClick={this.handleClick}>
          <LocationIndex selected={selected}>{index + 1}</LocationIndex>
          <LocationMessage selected={selected}>{message}</LocationMessage>
        </a>
      </div>
    );
  }
}
