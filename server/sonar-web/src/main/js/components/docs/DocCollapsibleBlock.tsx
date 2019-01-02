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
import OpenCloseIcon from '../icons-components/OpenCloseIcon';

interface State {
  open: boolean;
}

export default class DocCollapsibleBlock extends React.PureComponent<{}, State> {
  state = { open: false };

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    this.setState(state => ({ open: !state.open }));
    event.stopPropagation();
    event.preventDefault();
  };

  renderTitle(children: any) {
    return (
      <a
        aria-expanded={this.state.open}
        aria-haspopup={true}
        className="link-no-underline"
        href="#"
        onClick={this.handleClick}>
        <OpenCloseIcon className="vertical-middle little-spacer-right" open={this.state.open} />
        {children.props ? children.props.children : children}
      </a>
    );
  }

  render() {
    const childrenAsArray = React.Children.toArray(this.props.children);
    if (childrenAsArray.length < 1) {
      return null;
    }

    const firstChildChildren = React.Children.toArray(
      (childrenAsArray[0] as React.ReactElement<any>).props.children
    );
    if (firstChildChildren.length < 2) {
      return null;
    }

    return (
      <div className="collapse-container">
        {this.renderTitle(firstChildChildren[0])}
        {this.state.open && firstChildChildren.slice(1)}
      </div>
    );
  }
}
