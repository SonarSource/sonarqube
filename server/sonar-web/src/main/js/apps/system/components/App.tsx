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
import * as React from 'react';
import Helmet from 'react-helmet';
import ClusterSysInfos from './ClusterSysInfos';
import PageHeader from './PageHeader';
import StandAloneSysInfos from './StandAloneSysInfos';
import { translate } from '../../../helpers/l10n';
import { getSystemInfo } from '../../../api/system';
import { SysInfo } from '../types';
import '../styles.css';

interface State {
  loading: boolean;
  sysInfoData?: SysInfo;
}

export default class App extends React.PureComponent<any, State> {
  mounted: boolean;
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

  renderSysInfo() {
    const { sysInfoData } = this.state;
    if (!sysInfoData) {
      return null;
    }

    if (sysInfoData.cluster) {
      return <ClusterSysInfos sysInfoData={sysInfoData} />;
    }
    return <StandAloneSysInfos sysInfoData={sysInfoData} />;
  }

  render() {
    const { loading, sysInfoData } = this.state;
    const isCluster = sysInfoData != undefined && sysInfoData.cluster;
    // TODO Correctly get logLevel, we are not sure yet how we want to do it for cluster mode
    return (
      <div className="page page-limited">
        <Helmet title={translate('system_info.page')} />
        <PageHeader
          loading={loading}
          isCluster={isCluster}
          logLevel="INFO"
          showActions={sysInfoData != undefined}
        />
        {this.renderSysInfo()}
      </div>
    );
  }
}
