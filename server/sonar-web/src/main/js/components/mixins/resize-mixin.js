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
import ReactDOM from 'react-dom';

export const ResizeMixin = {
  componentDidMount() {
    if (this.isResizable()) {
      this.handleResize();
      window.addEventListener('resize', this.handleResize);
    }
  },

  componentWillUnmount() {
    if (this.isResizable()) {
      window.removeEventListener('resize', this.handleResize);
    }
  },

  handleResize() {
    const boundingClientRect = ReactDOM.findDOMNode(this).parentNode.getBoundingClientRect();
    const newWidth = this.props.width || boundingClientRect.width;
    const newHeight = this.props.height || boundingClientRect.height;
    this.setState({ width: newWidth, height: newHeight });
  },

  isResizable() {
    return !this.props.width || !this.props.height;
  }
};
