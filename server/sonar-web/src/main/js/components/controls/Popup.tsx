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
import OutsideClickHandler from './OutsideClickHandler';
import Tooltip from './Tooltip';

interface Props {
  children: (props: { onClick: () => void }) => React.ReactElement<any>;
  overlay: React.ReactNode;
}

interface State {
  visible: boolean;
}

export default class Popup extends React.Component<Props, State> {
  state: State = { visible: false };

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.overlay !== this.props.overlay) {
      this.setState({ visible: false });
    }
  }

  handleClick = (event?: React.MouseEvent<HTMLElement>) => {
    if (event) {
      event.preventDefault();
      event.currentTarget.blur();
    }

    // defer opening to not trigger OutsideClickHandler.onClickOutside callback
    setTimeout(() => {
      this.setState({ visible: true });
    }, 0);
  };

  handleClickOutside = () => {
    this.setState({ visible: false });
  };

  renderOverlay() {
    return (
      <OutsideClickHandler onClickOutside={this.handleClickOutside}>
        {({ ref }) => <div ref={ref}>{this.props.overlay}</div>}
      </OutsideClickHandler>
    );
  }

  render() {
    return (
      <Tooltip classNameSpace="popup" overlay={this.renderOverlay()} visible={this.state.visible}>
        {this.props.children({ onClick: this.handleClick })}
      </Tooltip>
    );
  }
}
