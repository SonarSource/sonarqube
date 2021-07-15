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
import * as React from 'react';
import { findDOMNode } from 'react-dom';

interface Props {
  children: React.ReactNode;
  onClickOutside: () => void;
}

export default class OutsideClickHandler extends React.Component<Props> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;
    setTimeout(() => {
      this.addClickHandler();
    }, 0);
  }

  componentWillUnmount() {
    this.mounted = false;
    this.removeClickHandler();
  }

  addClickHandler = () => {
    window.addEventListener('click', this.handleWindowClick);
  };

  removeClickHandler = () => {
    window.removeEventListener('click', this.handleWindowClick);
  };

  handleWindowClick = (event: MouseEvent) => {
    if (this.mounted) {
      // eslint-disable-next-line react/no-find-dom-node
      const node = findDOMNode(this);
      if (!node || !node.contains(event.target as Node)) {
        this.props.onClickOutside();
      }
    }
  };

  render() {
    return this.props.children;
  }
}
