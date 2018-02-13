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
import * as classNames from 'classnames';

interface Props {
  className?: string;
  children?: React.ReactNode;
  isOpen: boolean;
  offset?: { vertical: number; horizontal: number };
  popup: React.ReactElement<any>;
  position: 'bottomleft' | 'bottomright';
  togglePopup: (show: boolean) => void;
}

interface State {
  position: { top: number; left?: number; right?: number };
}

export default class BubblePopupHelper extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  popupContainer?: HTMLElement | null;
  state: State = {
    position: { top: 0, right: 0 }
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

  handleKey = (event: KeyboardEvent) => {
    // Escape key
    if (event.keyCode === 27) {
      this.props.togglePopup(false);
    }
  };

  handleOutsideClick = (event: MouseEvent) => {
    if (!this.popupContainer || !this.popupContainer.contains(event.target as Node)) {
      this.props.togglePopup(false);
    }
  };

  handleClick(event: React.SyntheticEvent<HTMLElement>) {
    event.stopPropagation();
  }

  getPosition(props: Props) {
    if (this.container) {
      const containerPos = this.container.getBoundingClientRect();
      const { position } = props;
      const offset = props.offset || { vertical: 0, horizontal: 0 };
      if (position === 'bottomleft') {
        return { top: containerPos.height + offset.vertical, left: offset.horizontal };
      } else {
        // if (position === 'bottomright')
        return { top: containerPos.height + offset.vertical, right: offset.horizontal };
      }
    } else {
      return { top: 0, right: 0 };
    }
  }

  render() {
    return (
      <div
        className={classNames(this.props.className, 'bubble-popup-helper')}
        ref={container => (this.container = container)}
        onClick={this.handleClick}
        tabIndex={0}
        role="tooltip">
        {this.props.children}
        {this.props.isOpen && (
          <div ref={popupContainer => (this.popupContainer = popupContainer)}>
            {React.cloneElement(this.props.popup, {
              popupPosition: this.state.position
            })}
          </div>
        )}
      </div>
    );
  }
}
