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
import React from 'react';
import classNames from 'classnames';

export default class Checkbox extends React.Component {
  static propTypes = {
    id: React.PropTypes.string,
    onCheck: React.PropTypes.func.isRequired,
    checked: React.PropTypes.bool.isRequired,
    thirdState: React.PropTypes.bool,
    className: React.PropTypes.string
  };

  static defaultProps = {
    thirdState: false
  };

  componentWillMount() {
    this.handleClick = this.handleClick.bind(this);
  }

  handleClick(e) {
    e.preventDefault();
    e.target.blur();
    this.props.onCheck(!this.props.checked, this.props.id);
  }

  render() {
    const className = classNames(this.props.className, 'icon-checkbox', {
      'icon-checkbox-checked': this.props.checked,
      'icon-checkbox-single': this.props.thirdState
    });

    return <a id={this.props.id} className={className} href="#" onClick={this.handleClick} />;
  }
}
