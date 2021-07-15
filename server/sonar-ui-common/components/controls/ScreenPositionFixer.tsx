/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { throttle } from 'lodash';
import * as React from 'react';
import { findDOMNode } from 'react-dom';
import { Theme, withTheme } from '../theme';

interface Props {
  /**
   * First time `children` are rendered with `undefined` fixes to measure the offset.
   * Second time it renders with the computed fixes.
   */
  children: (props: Fixes) => React.ReactNode;

  /**
   * Use this flag to force re-positioning.
   * Use cases:
   *   - when you need to measure `children` size first
   *   - when you load content asynchronously
   */
  ready?: boolean;
  theme: Theme;
}

interface Fixes {
  leftFix?: number;
  topFix?: number;
}

export class ScreenPositionFixer extends React.Component<Props, Fixes> {
  throttledPosition: () => void;

  constructor(props: Props) {
    super(props);
    this.state = {};
    this.throttledPosition = throttle(this.position, 50);
  }

  componentDidMount() {
    this.addEventListeners();
    this.position();
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.ready && this.props.ready) {
      this.position();
    } else if (prevProps.ready && !this.props.ready) {
      this.reset();
    }
  }

  componentWillUnmount() {
    this.removeEventListeners();
  }

  addEventListeners = () => {
    window.addEventListener('resize', this.throttledPosition);
  };

  removeEventListeners = () => {
    window.removeEventListener('resize', this.throttledPosition);
  };

  reset = () => {
    this.setState({ leftFix: undefined, topFix: undefined });
  };

  position = () => {
    const edgeMargin = 0.5 * this.props.theme.rawSizes.grid;

    // eslint-disable-next-line react/no-find-dom-node
    const node = findDOMNode(this);
    if (node && node instanceof Element) {
      const { width, height, left, top } = node.getBoundingClientRect();
      const { clientHeight, clientWidth } = document.documentElement;

      let leftFix = 0;
      if (left < edgeMargin) {
        leftFix = edgeMargin - left;
      } else if (left + width > clientWidth - edgeMargin) {
        leftFix = clientWidth - edgeMargin - left - width;
      }

      let topFix = 0;
      if (top < edgeMargin) {
        topFix = edgeMargin - top;
      } else if (top + height > clientHeight - edgeMargin) {
        topFix = clientHeight - edgeMargin - top - height;
      }

      this.setState({ leftFix, topFix });
    }
  };

  render() {
    return this.props.children(this.state);
  }
}

export default withTheme(ScreenPositionFixer);
