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
import * as classNames from 'classnames';
import ScreenPositionFixer from './ScreenPositionFixer';
import Toggler from './Toggler';
import { Popup, PopupPlacement } from '../ui/popups';

interface OnClickCallback {
  (event?: React.SyntheticEvent<HTMLElement>): void;
}

interface RenderProps {
  closeDropdown: () => void;
  onToggleClick: OnClickCallback;
  open: boolean;
}

interface Props {
  children:
    | ((renderProps: RenderProps) => JSX.Element)
    | React.ReactElement<{ onClick: OnClickCallback }>;
  className?: string;
  closeOnClick?: boolean;
  closeOnClickOutside?: boolean;
  onOpen?: () => void;
  overlay: React.ReactNode;
  overlayPlacement?: PopupPlacement;
  noOverlayPadding?: boolean;
  tagName?: string;
}

interface State {
  open: boolean;
}

export default class Dropdown extends React.PureComponent<Props, State> {
  state: State = { open: false };

  componentDidUpdate(_: Props, prevState: State) {
    if (!prevState.open && this.state.open && this.props.onOpen) {
      this.props.onOpen();
    }
  }

  closeDropdown = () => this.setState({ open: false });

  handleToggleClick = (event?: React.SyntheticEvent<HTMLElement>) => {
    if (event) {
      event.preventDefault();
      event.currentTarget.blur();
    }
    this.setState(state => ({ open: !state.open }));
  };

  render() {
    const a11yAttrs = {
      'aria-expanded': String(this.state.open),
      'aria-haspopup': 'true'
    };

    const child = React.isValidElement(this.props.children)
      ? React.cloneElement(this.props.children, { onClick: this.handleToggleClick, ...a11yAttrs })
      : this.props.children({
          closeDropdown: this.closeDropdown,
          onToggleClick: this.handleToggleClick,
          open: this.state.open
        });

    const { closeOnClick = true, closeOnClickOutside = false } = this.props;

    const toggler = (
      <Toggler
        closeOnClick={closeOnClick}
        closeOnClickOutside={closeOnClickOutside}
        onRequestClose={this.closeDropdown}
        open={this.state.open}
        overlay={
          <DropdownOverlay
            noPadding={this.props.noOverlayPadding}
            placement={this.props.overlayPlacement}>
            {this.props.overlay}
          </DropdownOverlay>
        }>
        {child}
      </Toggler>
    );

    return React.createElement(
      this.props.tagName || 'div',
      { className: classNames('dropdown', this.props.className) },
      toggler
    );
  }
}

interface OverlayProps {
  className?: string;
  children: React.ReactNode;
  noPadding?: boolean;
  placement?: PopupPlacement;
}

// TODO use the same styling for <Select />
// TODO use the same styling for <DateInput />

export class DropdownOverlay extends React.Component<OverlayProps> {
  get placement() {
    return this.props.placement || PopupPlacement.Bottom;
  }

  renderPopup = (leftFix?: number, topFix?: number) => (
    <Popup
      arrowStyle={
        leftFix !== undefined && topFix !== undefined
          ? { transform: `translate(${-leftFix}px, ${-topFix}px)` }
          : undefined
      }
      className={this.props.className}
      noPadding={this.props.noPadding}
      placement={this.placement}
      style={
        leftFix !== undefined && topFix !== undefined
          ? { marginLeft: `calc(50% + ${leftFix}px)` }
          : undefined
      }>
      {this.props.children}
    </Popup>
  );

  render() {
    if (this.placement === PopupPlacement.Bottom) {
      return (
        <ScreenPositionFixer>
          {({ leftFix, topFix }) => this.renderPopup(leftFix, topFix)}
        </ScreenPositionFixer>
      );
    } else {
      return this.renderPopup();
    }
  }
}
