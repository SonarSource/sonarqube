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
import { withRouter, WithRouterProps } from 'react-router';
import Helmet from 'react-helmet';
import ClusterSysInfos from './ClusterSysInfos';
import PageHeader from './PageHeader';
import StandaloneSysInfos from './StandaloneSysInfos';
import SystemUpgradeNotif from './system-upgrade/SystemUpgradeNotif';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { translate } from '../../../helpers/l10n';
import { ClusterSysInfo, getSystemInfo, SysInfo } from '../../../api/system';
import {
  getServerId,
  getSystemLogsLevel,
  isCluster,
  parseQuery,
  Query,
  serializeQuery
} from '../utils';
import '../styles.css';

type Props = WithRouterProps;

interface State {
  loading: boolean;
  sysInfoData?: SysInfo;
}

class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchSysInfo();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSysInfo = () => {
    this.setState({ loading: true });
    getSystemInfo().then(
      (sysInfoData: SysInfo) => {
        if (this.mounted) {
          this.setState({ loading: false, sysInfoData });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  toggleSysInfoCards = (toggledCard: string) => {
    const query = parseQuery(this.props.location.query);
    let expandedCards;
    if (query.expandedCards.includes(toggledCard)) {
      expandedCards = query.expandedCards.filter(card => card !== toggledCard);
    } else {
      expandedCards = query.expandedCards.concat(toggledCard);
    }
    this.updateQuery({ expandedCards });
  };

  updateQuery = (newQuery: Query) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.props.router.replace({ pathname: this.props.location.pathname, query });
  };

  renderSysInfo() {
    const { sysInfoData } = this.state;
    if (!sysInfoData) {
      return null;
    }

    const query = parseQuery(this.props.location.query);
    if (isCluster(sysInfoData)) {
      return (
        <ClusterSysInfos
          expandedCards={query.expandedCards}
          sysInfoData={sysInfoData as ClusterSysInfo}
          toggleCard={this.toggleSysInfoCards}
        />
      );
    }
    return (
      <StandaloneSysInfos
        expandedCards={query.expandedCards}
        sysInfoData={sysInfoData}
        toggleCard={this.toggleSysInfoCards}
      />
    );
  }

  render() {
    const { loading, sysInfoData } = this.state;
    return (
      <div className="page page-limited">
        <Suggestions suggestions="system_info" />
        <Helmet title={translate('system_info.page')} />
        <SystemUpgradeNotif />
        <PageHeader
          isCluster={isCluster(sysInfoData)}
          loading={loading}
          logLevel={getSystemLogsLevel(sysInfoData)}
          onLogLevelChange={this.fetchSysInfo}
          serverId={getServerId(sysInfoData)}
          showActions={sysInfoData !== undefined}
        />
        {this.renderSysInfo()}
      </div>
    );
  }
}

export default withRouter(App);
