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
import { connect } from 'react-redux';
import Header from './Header';
import Form from './Form';
import { getComponent } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';

class Deletion extends React.PureComponent {
  static propTypes = {
    component: React.PropTypes.object
  };

  render() {
    if (!this.props.component) {
      return null;
    }

    return (
      <div className="page page-limited">
        <Helmet title={translate('deletion.page')} />
        <Header component={this.props.component} />
        <Form component={this.props.component} />
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  component: getComponent(state, ownProps.location.query.id)
});

export default connect(mapStateToProps)(Deletion);
