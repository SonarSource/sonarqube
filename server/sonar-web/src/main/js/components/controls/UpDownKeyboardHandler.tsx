/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { isShortcut, isTextarea } from '../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../helpers/keycodes';

interface Props {
  containerClass?: string;
}

interface State {
  focusIndex?: number;
}

export default class UpDownKeyboardHanlder extends React.PureComponent<
  React.PropsWithChildren<Props>,
  State
> {
  constructor(props: React.PropsWithChildren<Props>) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    document.addEventListener('keydown', this.handleKeyboard, true);
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleKeyboard, true);
  }

  handleKeyboard = (event: KeyboardEvent) => {
    if (isShortcut(event) || isTextarea(event)) {
      return true;
    }
    switch (event.key) {
      case KeyboardKeys.DownArrow:
        event.stopPropagation();
        event.preventDefault();
        this.selectNextFocusElement();
        return false;
      case KeyboardKeys.UpArrow:
        event.stopPropagation();
        event.preventDefault();
        this.selectPreviousFocusElement();
        return false;
    }
    return true;
  };

  getFocusableElement() {
    const { containerClass = 'popup' } = this.props;
    const focussableElements = `.${containerClass} a,.${containerClass} button,.${containerClass} input[type=text],.${containerClass} textarea`;
    return document.querySelectorAll<HTMLElement>(focussableElements);
  }

  selectNextFocusElement() {
    const { focusIndex = -1 } = this.state;
    const focusableElement = this.getFocusableElement();

    for (const [index, focusable] of focusableElement.entries()) {
      if (focusable === document.activeElement) {
        focusableElement[(index + 1) % focusableElement.length].focus();
        this.setState({ focusIndex: (index + 1) % focusableElement.length });
        return;
      }
    }

    if (focusableElement[(focusIndex + 1) % focusableElement.length]) {
      focusableElement[(focusIndex + 1) % focusableElement.length].focus();
      this.setState({ focusIndex: (focusIndex + 1) % focusableElement.length });
    }
  }

  selectPreviousFocusElement() {
    const { focusIndex = 0 } = this.state;
    const focusableElement = this.getFocusableElement();

    for (const [index, focusable] of focusableElement.entries()) {
      if (focusable === document.activeElement) {
        focusableElement[(index - 1 + focusableElement.length) % focusableElement.length].focus();
        this.setState({
          focusIndex: (index - 1 + focusableElement.length) % focusableElement.length,
        });
        return;
      }
    }

    if (focusableElement[(focusIndex - 1 + focusableElement.length) % focusableElement.length]) {
      focusableElement[
        (focusIndex - 1 + focusableElement.length) % focusableElement.length
      ].focus();
      this.setState({
        focusIndex: (focusIndex - 1 + focusableElement.length) % focusableElement.length,
      });
    }
  }

  render() {
    return this.props.children;
  }
}
