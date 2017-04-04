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
import React from 'react';
import classNames from 'classnames';

type Props = {
  className?: string,
  children: React.Component<*>,
  isOpen: boolean,
  offset?: {
    vertical: number,
    horizontal: number
  },
  popup: React.Component<*>,
  position: 'bottomleft' | 'bottomright',
  togglePopup: (?boolean) => void
};

type State = {
  position: { top: number, right: number }
};

export default class BubblePopupHelper extends React.PureComponent {
  props: Props;
  state: State = {
    position: {
      top: 0,
      right: 0
    }
  };

  componentDidMount() {
    this.setState({ position: this.getPosition(this.props) });
  }

  componentWillReceiveProps(nextProps: Props) {
    if (!this.props.isOpen && nextProps.isOpen) {
      window.addEventListener('keydown', this.handleKey, false);
      window.addEventListener('click', this.handleOutsideClick, false);
    } else if (this.props.isOpen && !nextProps.isOpen) {
      window.removeEventListener('keydown', this.handleKey);
      window.removeEventListener('click', this.handleOutsideClick);
    }
  }

  handleKey = (evt: KeyboardEvent) => {
    // Escape key
    if (evt.keyCode === 27) {
      this.props.togglePopup(false);
    }
  };

  handleOutsideClick = (evt: SyntheticInputEvent) => {
    if (!this.popupContainer || !this.popupContainer.contains(evt.target)) {
      this.props.togglePopup(false);
    }
  };

  handleClick(evt: SyntheticInputEvent) {
    evt.stopPropagation();
  }

  getPosition(props: Props) {
    const containerPos = this.container.getBoundingClientRect();
    const { position } = props;
    const offset = props.offset || { vertical: 0, horizontal: 0 };
    if (position === 'bottomleft') {
      return { top: containerPos.height + offset.vertical, left: offset.horizontal };
    } else if (position === 'bottomright') {
      return { top: containerPos.height + offset.vertical, right: offset.horizontal };
    }
  }

  render() {
    return (
      <div
        className={classNames(this.props.className, 'bubble-popup-helper')}
        ref={container => this.container = container}
        onClick={this.handleClick}
        tabIndex={0}
        role="tooltip">
        {this.props.children}
        {this.props.isOpen &&
          <div ref={popupContainer => this.popupContainer = popupContainer}>
            {React.cloneElement(this.props.popup, {
              popupPosition: this.state.position
            })}
          </div>}
      </div>
    );
  }
}
