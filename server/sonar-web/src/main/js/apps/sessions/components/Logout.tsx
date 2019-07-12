/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import GlobalMessagesContainer from '../../../app/components/GlobalMessagesContainer';
import RecentHistory from '../../../app/components/RecentHistory';
import { doLogout } from '../../../store/rootActions';

interface Props {
  doLogout: () => Promise<void>;
}

export class Logout extends React.PureComponent<Props> {
  componentDidMount() {
    this.props.doLogout().then(
      () => {
        RecentHistory.clear();
        window.location.replace(getBaseUrl() + '/');
      },
      () => {}
    );
  }

  render() {
    return (
      <div className="page page-limited">
        <GlobalMessagesContainer />
        <div className="text-center">{translate('logging_out')}</div>
      </div>
    );
  }
}

const mapStateToProps = () => ({});

const mapDispatchToProps = { doLogout };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Logout as any);
