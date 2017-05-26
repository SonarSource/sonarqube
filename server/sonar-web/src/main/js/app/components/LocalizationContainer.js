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
import GlobalLoading from './GlobalLoading';
import { requestMessages } from '../../helpers/l10n';

export default class LocalizationContainer extends React.PureComponent {
  mounted: boolean;

  state = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    requestMessages().then(this.finishLoading, this.finishLoading);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  finishLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    if (this.state.loading) {
      return <GlobalLoading />;
    }
    return this.props.children;
  }
}
