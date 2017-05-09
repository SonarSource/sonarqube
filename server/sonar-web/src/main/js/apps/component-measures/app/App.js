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
import Helmet from 'react-helmet';
import Spinner from './../components/Spinner';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

export default class App extends React.PureComponent {
  state = { componentSet: false };

  componentDidMount() {
    this.props.setComponent(this.props.component);
    this.props.fetchMetrics();
    this.setState({ componentSet: true });
  }

  render() {
    if (this.props.metrics == null || !this.state.componentSet) {
      return <Spinner />;
    }

    return (
      <main id="component-measures">
        <Helmet title={translate('layout.measures')} />
        {this.props.children}
      </main>
    );
  }
}
