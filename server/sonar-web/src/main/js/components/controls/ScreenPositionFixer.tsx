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
import { findDOMNode } from 'react-dom';
import { throttle } from 'lodash';
import { grid } from '../../app/theme';

const EDGE_MARGIN = 0.5 * grid;

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
}

interface Fixes {
  leftFix?: number;
  topFix?: number;
}

export default class ScreenPositionFixer extends React.Component<Props, Fixes> {
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

  position = () => {
    // eslint-disable-next-line react/no-find-dom-node
    const node = findDOMNode(this);
    if (node && node instanceof Element) {
      const { width, height, left, top } = node.getBoundingClientRect();
      const { clientHeight, clientWidth } = document.body;

      let leftFix = 0;
      if (left < EDGE_MARGIN) {
        leftFix = EDGE_MARGIN - left;
      } else if (left + width > clientWidth - EDGE_MARGIN) {
        leftFix = clientWidth - EDGE_MARGIN - left - width;
      }

      let topFix = 0;
      if (top < EDGE_MARGIN) {
        topFix = EDGE_MARGIN - top;
      } else if (top + height > clientHeight - EDGE_MARGIN) {
        topFix = clientHeight - EDGE_MARGIN - top - height;
      }

      this.setState({ leftFix, topFix });
    }
  };

  render() {
    return this.props.children(this.state);
  }
}
