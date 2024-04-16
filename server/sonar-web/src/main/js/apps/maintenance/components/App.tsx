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

import styled from '@emotion/styled';
import { Link, LinkStandalone, Spinner } from '@sonarsource/echoes-react';
import { ButtonPrimary, Card, CenteredLayout, Note, Title } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { getMigrationStatus, getSystemStatus, migrateDatabase } from '../../../api/system';
import DocumentationLink from '../../../components/common/DocumentationLink';
import InstanceMessage from '../../../components/common/InstanceMessage';
import DateFromNow from '../../../components/intl/DateFromNow';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { isDefined } from '../../../helpers/types';
import { getReturnUrl } from '../../../helpers/urls';

interface Props {
  location: { query?: { return_to?: string } };
  setup: boolean;
}

interface State {
  message?: string;
  startedAt?: string;
  state?: string;
  status?: string;
  wasStarting?: boolean;
}

const DELAY_REDIRECT_PREV_PAGE = 2500;
const DELAY_REFRESH_STATUS = 5000;

export default class App extends React.PureComponent<Props, State> {
  interval?: number;
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchStatus();
  }

  componentWillUnmount() {
    this.mounted = false;

    if (isDefined(this.interval)) {
      window.clearInterval(this.interval);
    }
  }

  fetchStatus = () => {
    const request = this.props.setup ? this.fetchMigrationState() : this.fetchSystemStatus();

    request.catch(() => {
      if (this.mounted) {
        this.setState({
          message: undefined,
          startedAt: undefined,
          state: undefined,
          status: 'OFFLINE',
        });
      }
    });
  };

  fetchSystemStatus = () => {
    return getSystemStatus().then(({ status }) => {
      if (this.mounted) {
        this.setState({ status });

        if (status === 'STARTING') {
          this.setState({ wasStarting: true });
          this.scheduleRefresh();
        } else if (status === 'UP') {
          if (this.state.wasStarting) {
            this.loadPreviousPage();
          }
        } else {
          this.scheduleRefresh();
        }
      }
    });
  };

  fetchMigrationState = () => {
    return getMigrationStatus().then(({ message, startedAt, state }) => {
      if (this.mounted) {
        this.setState({ message, startedAt, state });
        if (state === 'MIGRATION_SUCCEEDED') {
          this.loadPreviousPage();
        } else if (state !== 'NO_MIGRATION') {
          this.scheduleRefresh();
        }
      }
    });
  };

  scheduleRefresh = () => {
    this.interval = window.setTimeout(this.fetchStatus, DELAY_REFRESH_STATUS);
  };

  loadPreviousPage = () => {
    setInterval(() => {
      window.location.replace(getReturnUrl(this.props.location));
    }, DELAY_REDIRECT_PREV_PAGE);
  };

  handleMigrateClick = () => {
    migrateDatabase().then(
      ({ message, startedAt, state }) => {
        if (this.mounted) {
          this.setState({ message, startedAt, state });
        }
      },
      () => {},
    );
  };

  render() {
    const { state, status } = this.state;

    return (
      <>
        <Helmet defaultTitle={translate('maintenance.page')} defer={false} />

        <CenteredLayout className="sw-flex sw-justify-around sw-mt-32" id="bd">
          <Card className="sw-body-sm sw-p-10 sw-w-abs-400" id="nonav">
            {status === 'OFFLINE' && (
              <>
                <MaintenanceTitle className="text-danger">
                  <InstanceMessage message={translate('maintenance.is_offline')} />
                </MaintenanceTitle>

                <MaintenanceText>
                  {translate('maintenance.sonarqube_is_offline.text')}
                </MaintenanceText>

                <div className="sw-text-center">
                  <LinkStandalone reloadDocument to={`${getBaseUrl()}/`}>
                    {translate('maintenance.try_again')}
                  </LinkStandalone>
                </div>
              </>
            )}

            {status === 'UP' && (
              <>
                <MaintenanceTitle>
                  <InstanceMessage message={translate('maintenance.is_up')} />
                </MaintenanceTitle>

                <MaintenanceText className="sw-text-center">
                  {translate('maintenance.all_systems_opetational')}
                </MaintenanceText>

                <div className="sw-text-center">
                  <LinkStandalone to="/">{translate('layout.home')}</LinkStandalone>
                </div>
              </>
            )}

            {status === 'STARTING' && (
              <>
                <MaintenanceTitle>
                  <InstanceMessage message={translate('maintenance.is_starting')} />
                </MaintenanceTitle>

                <MaintenanceSpinner>
                  <Spinner />
                </MaintenanceSpinner>
              </>
            )}

            {status === 'DOWN' && (
              <>
                <MaintenanceTitle className="text-danger">
                  <InstanceMessage message={translate('maintenance.is_down')} />
                </MaintenanceTitle>

                <MaintenanceText>{translate('maintenance.sonarqube_is_down.text')}</MaintenanceText>

                <MaintenanceText className="sw-text-center">
                  <LinkStandalone reloadDocument to={`${getBaseUrl()}/`}>
                    {translate('maintenance.try_again')}
                  </LinkStandalone>
                </MaintenanceText>
              </>
            )}

            {(status === 'DB_MIGRATION_NEEDED' || status === 'DB_MIGRATION_RUNNING') && (
              <>
                <MaintenanceTitle>
                  <InstanceMessage message={translate('maintenance.is_under_maintenance')} />
                </MaintenanceTitle>

                <MaintenanceText>
                  <FormattedMessage
                    defaultMessage={translate('maintenance.sonarqube_is_under_maintenance.1')}
                    id="maintenance.sonarqube_is_under_maintenance.1"
                    values={{
                      link: (
                        <Link to="https://www.sonarlint.org/?referrer=sonarqube-maintenance">
                          {translate('maintenance.sonarqube_is_under_maintenance_link.1')}
                        </Link>
                      ),
                    }}
                  />
                </MaintenanceText>

                <MaintenanceText>
                  <FormattedMessage
                    defaultMessage={translate('maintenance.sonarqube_is_under_maintenance.2')}
                    id="maintenance.sonarqube_is_under_maintenance.2"
                    values={{
                      link: (
                        <DocumentationLink to="/setup-and-upgrade/upgrade-the-server/roadmap/">
                          {translate('maintenance.sonarqube_is_under_maintenance_link.2')}
                        </DocumentationLink>
                      ),
                    }}
                  />
                </MaintenanceText>
              </>
            )}

            {state === 'NO_MIGRATION' && (
              <>
                <MaintenanceTitle>
                  {translate('maintenance.database_is_up_to_date')}
                </MaintenanceTitle>

                <div className="sw-text-center">
                  <LinkStandalone to="/">{translate('layout.home')}</LinkStandalone>
                </div>
              </>
            )}

            {state === 'MIGRATION_REQUIRED' && (
              <>
                <MaintenanceTitle>{translate('maintenance.upgrade_database')}</MaintenanceTitle>

                <MaintenanceText>{translate('maintenance.upgrade_database.1')}</MaintenanceText>

                <MaintenanceText>{translate('maintenance.upgrade_database.2')}</MaintenanceText>

                <MaintenanceText>{translate('maintenance.upgrade_database.3')}</MaintenanceText>

                <MaintenanceSpinner>
                  <ButtonPrimary id="start-migration" onClick={this.handleMigrateClick}>
                    {translate('maintenance.upgrade')}
                  </ButtonPrimary>
                </MaintenanceSpinner>
              </>
            )}

            {state === 'NOT_SUPPORTED' && (
              <>
                <MaintenanceTitle className="text-danger">
                  {translate('maintenance.migration_not_supported')}
                </MaintenanceTitle>

                <p>{translate('maintenance.migration_not_supported.text')}</p>
              </>
            )}

            {state === 'MIGRATION_RUNNING' && (
              <>
                <MaintenanceTitle>{translate('maintenance.database_migration')}</MaintenanceTitle>

                {isDefined(this.state.message) && (
                  <MaintenanceText className="sw-text-center">{this.state.message}</MaintenanceText>
                )}

                {isDefined(this.state.startedAt) && (
                  <MaintenanceText className="sw-text-center">
                    {translate('background_tasks.table.started')}{' '}
                    <DateFromNow date={this.state.startedAt} />
                    <br />
                    <Note>
                      <TimeFormatter date={this.state.startedAt} long />
                    </Note>
                  </MaintenanceText>
                )}

                <MaintenanceSpinner>
                  <Spinner />
                </MaintenanceSpinner>
              </>
            )}

            {state === 'MIGRATION_SUCCEEDED' && (
              <>
                <MaintenanceTitle className="text-success">
                  {translate('maintenance.database_is_up_to_date')}
                </MaintenanceTitle>

                <div className="sw-text-center">
                  <LinkStandalone to="/">{translate('layout.home')}</LinkStandalone>
                </div>
              </>
            )}

            {state === 'MIGRATION_FAILED' && (
              <>
                <MaintenanceTitle className="text-danger">
                  {translate('maintenance.upgrade_failed')}
                </MaintenanceTitle>

                <MaintenanceText>{translate('maintenance.upgrade_failed.text')}</MaintenanceText>
              </>
            )}
          </Card>
        </CenteredLayout>
      </>
    );
  }
}

const MaintenanceTitle = styled(Title)`
  margin-bottom: 2.5rem;
  text-align: center;
`;

const MaintenanceText = styled.p`
  margin-bottom: 1rem;
`;

const MaintenanceSpinner = styled.div`
  margin-top: 2.5rem;
  text-align: center;
`;
