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
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import GlobalNotifications from './GlobalNotifications';
import Projects from './Projects';
import { fetchNotifications } from './actions';
import { translate } from '../../../helpers/l10n';

class Notifications extends React.PureComponent {
  /*:: props: {
    fetchNotifications: () => void
  };
*/

  componentDidMount() {
    this.props.fetchNotifications();
  }

  render() {
    return (
      <div className="account-body account-container">
        <Helmet title={translate('my_account.notifications')} />
        <p className="alert alert-info">{translate('notification.dispatcher.information')}</p>
        <GlobalNotifications />
        <Projects />
      </div>
    );
  }
}

const mapDispatchToProps = { fetchNotifications };

export default connect(null, mapDispatchToProps)(Notifications);

export const UnconnectedNotifications = Notifications;
