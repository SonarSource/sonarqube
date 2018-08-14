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
import { findDOMNode } from 'react-dom';

interface Props {
  delay?: number;
  onOver: () => void;
}

export default class MouseOverHandler extends React.Component<Props> {
  mouseEnterInterval?: number;
  mounted = false;

  componentDidMount() {
    this.mounted = true;

    const node = this.getNode();
    if (node) {
      this.attachEvents(node);
    }
  }

  componentWillUnmount() {
    this.mounted = false;

    const node = this.getNode();
    if (node) {
      this.detachEvents(node);
    }
  }

  getNode = () => {
    // eslint-disable-next-line react/no-find-dom-node
    const node = findDOMNode(this);
    return node && node instanceof Element ? node : undefined;
  };

  attachEvents = (node: Element) => {
    node.addEventListener('mouseenter', this.handleMouseEnter);
    node.addEventListener('mouseleave', this.handleMouseLeave);
  };

  detachEvents = (node: Element) => {
    node.removeEventListener('mouseenter', this.handleMouseEnter);
    node.removeEventListener('mouseleave', this.handleMouseLeave);
  };

  handleMouseEnter = () => {
    this.mouseEnterInterval = window.setTimeout(() => {
      if (this.mounted) {
        this.props.onOver();
      }
    }, this.props.delay || 0);
  };

  handleMouseLeave = () => {
    if (this.mouseEnterInterval !== undefined) {
      window.clearInterval(this.mouseEnterInterval);
      this.mouseEnterInterval = undefined;
    }
  };

  render() {
    return this.props.children;
  }
}
