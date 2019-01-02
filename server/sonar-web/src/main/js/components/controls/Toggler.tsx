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
import DocumentClickHandler from './DocumentClickHandler';
import OutsideClickHandler from './OutsideClickHandler';

export interface Props {
  children?: React.ReactNode;
  closeOnClick?: boolean;
  closeOnClickOutside?: boolean;
  closeOnEscape?: boolean;
  onRequestClose: () => void;
  open: boolean;
  overlay: React.ReactNode;
}

export default class Toggler extends React.Component<Props> {
  componentDidMount() {
    if (this.props.open && isTrueOrUndefined(this.props.closeOnEscape)) {
      this.addEventListeners();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.open && this.props.open && isTrueOrUndefined(this.props.closeOnEscape)) {
      this.addEventListeners();
    } else if (prevProps.open && !this.props.open) {
      this.removeEventListeners();
    } else if (
      isTrueOrUndefined(prevProps.closeOnEscape) &&
      !isTrueOrUndefined(this.props.closeOnEscape)
    ) {
      this.removeEventListeners();
    }
  }

  componentWillUnmount() {
    this.removeEventListeners();
  }

  addEventListeners() {
    document.addEventListener('keydown', this.handleKeyDown, false);
  }

  removeEventListeners() {
    document.removeEventListener('keydown', this.handleKeyDown, false);
  }

  handleKeyDown = (event: KeyboardEvent) => {
    // Escape key
    if (event.keyCode === 27) {
      this.props.onRequestClose();
    }
  };

  renderOverlay() {
    const {
      closeOnClick = false,
      closeOnClickOutside = true,
      onRequestClose,
      overlay
    } = this.props;

    if (closeOnClick) {
      return <DocumentClickHandler onClick={onRequestClose}>{overlay}</DocumentClickHandler>;
    } else if (closeOnClickOutside) {
      return <OutsideClickHandler onClickOutside={onRequestClose}>{overlay}</OutsideClickHandler>;
    } else {
      return overlay;
    }
  }

  render() {
    return (
      <>
        {this.props.children}
        {this.props.open && this.renderOverlay()}
      </>
    );
  }
}

function isTrueOrUndefined(x: boolean | undefined) {
  return x === true || x === undefined;
}
