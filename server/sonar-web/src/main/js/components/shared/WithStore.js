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
// @flow
import React from 'react';
import PropTypes from 'prop-types';
import getStore from '../../app/utils/getStore';

/*::
type Props = {
  children: React.Element<*>
};
*/

export default class WithStore extends React.PureComponent {
  /*:: props: Props; */
  /*:: store: {};
*/

  static childContextTypes = {
    store: PropTypes.object
  };

  constructor(props /*: Props */) {
    super(props);
    this.store = getStore();
  }

  getChildContext() {
    return { store: this.store };
  }

  render() {
    return this.props.children;
  }
}
