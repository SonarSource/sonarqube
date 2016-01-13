/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';

export default React.createClass({
  propTypes: {
    onCheck: React.PropTypes.func.isRequired,
    initiallyChecked: React.PropTypes.bool,
    thirdState: React.PropTypes.bool
  },

  getInitialState() {
    return { checked: this.props.initiallyChecked || false };
  },

  componentWillReceiveProps(nextProps) {
    if (nextProps.initiallyChecked != null) {
      this.setState({ checked: nextProps.initiallyChecked });
    }
  },

  toggle(e) {
    e.preventDefault();
    this.props.onCheck(!this.state.checked);
    this.setState({ checked: !this.state.checked });
  },

  render() {
    let classNames = ['icon-checkbox'];
    if (this.state.checked) {
      classNames.push('icon-checkbox-checked');
    }
    if (this.props.thirdState) {
      classNames.push('icon-checkbox-single');
    }
    let className = classNames.join(' ');
    return <a onClick={this.toggle} className={className} href="#"/>;
  }
});
