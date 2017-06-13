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
// @flow
import React from 'react';
import ReactDOM from 'react-dom';

type Props = {
  children: React.Element<*>,
  height?: number,
  width?: number
};

type State = {
  height?: number,
  width?: number
};

export default class ResizeHelper extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { height: props.height, width: props.width };
  }

  componentDidMount() {
    if (this.isResizable()) {
      this.handleResize();
      window.addEventListener('resize', this.handleResize);
    }
  }

  componentWillUnmount() {
    if (this.isResizable()) {
      window.removeEventListener('resize', this.handleResize);
    }
  }

  isResizable = () => {
    return !this.props.width || !this.props.height;
  };

  handleResize = () => {
    const domNode = ReactDOM.findDOMNode(this);
    if (domNode && domNode.parentElement) {
      const boundingClientRect = domNode.parentElement.getBoundingClientRect();
      this.setState({ width: boundingClientRect.width, height: boundingClientRect.height });
    }
  };

  render() {
    return React.cloneElement(this.props.children, {
      width: this.props.width || this.state.width,
      height: this.props.height || this.state.height
    });
  }
}
