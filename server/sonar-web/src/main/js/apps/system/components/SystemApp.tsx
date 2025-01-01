/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import { LargeCenteredLayout, PageContentFontWrapper } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, Router } from '~sonar-aligned/types/router';
import { getSystemInfo } from '../../../api/system';
import { UpdateNotification } from '../../../app/components/update-notification/UpdateNotification';
import { translate } from '../../../helpers/l10n';
import { SysInfoCluster, SysInfoStandalone } from '../../../types/types';
import '../styles.css';
import {
  Query,
  getClusterVersion,
  getServerId,
  getSystemLogsLevel,
  getVersion,
  isCluster,
  parseQuery,
  serializeQuery,
} from '../utils';
import ClusterSysInfos from './ClusterSysInfos';
import PageHeader from './PageHeader';
import StandaloneSysInfos from './StandaloneSysInfos';

interface Props {
  location: Location;
  router: Router;
}

interface State {
  loading: boolean;
  sysInfoData?: SysInfoCluster | SysInfoStandalone;
}

class SystemApp extends React.PureComponent<Props, State> {
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
      (sysInfoData) => {
        if (this.mounted) {
          this.setState({ loading: false, sysInfoData });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  toggleSysInfoCards = (toggledCard: string) => {
    const query = parseQuery(this.props.location.query);
    let expandedCards;
    if (query.expandedCards.includes(toggledCard)) {
      expandedCards = query.expandedCards.filter((card) => card !== toggledCard);
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
          sysInfoData={sysInfoData}
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
      <LargeCenteredLayout as="main">
        <Helmet defer={false} title={translate('system_info.page')} />
        <PageContentFontWrapper className="sw-typo-default sw-pb-8">
          <div>
            <UpdateNotification />
          </div>
          {sysInfoData && (
            <PageHeader
              isCluster={isCluster(sysInfoData)}
              loading={loading}
              logLevel={getSystemLogsLevel(sysInfoData)}
              onLogLevelChange={this.fetchSysInfo}
              serverId={getServerId(sysInfoData)}
              version={
                isCluster(sysInfoData) ? getClusterVersion(sysInfoData) : getVersion(sysInfoData)
              }
            />
          )}
          {this.renderSysInfo()}
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withRouter(SystemApp);
