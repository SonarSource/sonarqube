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
import { Button, ButtonVariety, Link, LinkStandalone, Spinner } from '@sonarsource/echoes-react';
import { Card, CenteredLayout, Note, Title } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { getMigrationsStatus, getSystemStatus, migrateDatabase } from '../../../api/system';
import DocumentationLink from '../../../components/common/DocumentationLink';
import InstanceMessage from '../../../components/common/InstanceMessage';
import DateFromNow from '../../../components/intl/DateFromNow';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { isDefined } from '../../../helpers/types';
import { getReturnUrl } from '../../../helpers/urls';
import { MigrationStatus } from '../../../types/system';
import { MigrationProgress } from './MigrationProgress';

interface Props {
  location: { query?: { return_to?: string } };
  setup: boolean;
}

interface State {
  message?: string;
  migrationState?: MigrationStatus;
  progress?: {
    completedSteps: number;
    expectedFinishTimestamp: string;
    totalSteps: number;
  };
  startedAt?: string;
  systemStatus?: string;
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
          migrationState: undefined,
          systemStatus: 'OFFLINE',
        });
      }
    });
  };

  fetchSystemStatus = () => {
    return getSystemStatus().then(({ status: systemStatus }) => {
      if (this.mounted) {
        this.setState({ systemStatus });

        if (systemStatus === 'STARTING') {
          this.setState({ wasStarting: true });
          this.scheduleRefresh();
        } else if (systemStatus === 'UP') {
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
    return getMigrationsStatus().then(
      ({
        message,
        startedAt,
        status: migrationState,
        completedSteps,
        totalSteps,
        expectedFinishTimestamp,
      }) => {
        if (this.mounted) {
          const progress =
            isDefined(completedSteps) && isDefined(totalSteps) && isDefined(expectedFinishTimestamp)
              ? {
                  completedSteps,
                  totalSteps,
                  expectedFinishTimestamp,
                }
              : undefined;

          this.setState({
            message,
            startedAt,
            migrationState,
            progress,
          });
          if (migrationState === 'MIGRATION_SUCCEEDED') {
            this.loadPreviousPage();
          } else if (migrationState !== 'NO_MIGRATION') {
            this.scheduleRefresh();
          }
        }
      },
    );
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
          this.setState({ message, startedAt, migrationState: state });
        }
      },
      () => {},
    );
  };

  render() {
    const { migrationState, systemStatus, progress } = this.state;

    return (
      <>
        <Helmet defaultTitle={translate('maintenance.page')} defer={false} />

        <CenteredLayout className="sw-flex sw-justify-around sw-mt-32" id="bd">
          <Card className="sw-typo-default sw-p-10 sw-w-abs-400" id="nonav">
            {systemStatus === 'OFFLINE' && (
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

            {systemStatus === 'UP' && (
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

            {systemStatus === 'STARTING' && (
              <>
                <MaintenanceTitle>
                  <InstanceMessage message={translate('maintenance.is_starting')} />
                </MaintenanceTitle>

                <MaintenanceSpinner>
                  <Spinner />
                </MaintenanceSpinner>
              </>
            )}

            {systemStatus === 'DOWN' && (
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

            {(systemStatus === 'DB_MIGRATION_NEEDED' ||
              systemStatus === 'DB_MIGRATION_RUNNING') && (
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
                        <DocumentationLink to={DocLink.ServerUpgradeRoadmap}>
                          {translate('maintenance.sonarqube_is_under_maintenance_link.2')}
                        </DocumentationLink>
                      ),
                    }}
                  />
                </MaintenanceText>
              </>
            )}

            {migrationState === MigrationStatus.noMigration && (
              <>
                <MaintenanceTitle>
                  {translate('maintenance.database_is_up_to_date')}
                </MaintenanceTitle>

                <div className="sw-text-center">
                  <LinkStandalone to="/">{translate('layout.home')}</LinkStandalone>
                </div>
              </>
            )}

            {migrationState === MigrationStatus.required && (
              <>
                <MaintenanceTitle>{translate('maintenance.upgrade_database')}</MaintenanceTitle>

                <MaintenanceText>{translate('maintenance.upgrade_database.1')}</MaintenanceText>

                <MaintenanceText>{translate('maintenance.upgrade_database.2')}</MaintenanceText>

                <MaintenanceText>{translate('maintenance.upgrade_database.3')}</MaintenanceText>

                <MaintenanceSpinner>
                  <Button
                    id="start-migration"
                    onClick={this.handleMigrateClick}
                    variety={ButtonVariety.Primary}
                  >
                    {translate('maintenance.upgrade')}
                  </Button>
                </MaintenanceSpinner>
              </>
            )}

            {migrationState === MigrationStatus.notSupported && (
              <>
                <MaintenanceTitle className="text-danger">
                  {translate('maintenance.migration_not_supported')}
                </MaintenanceTitle>

                <p>{translate('maintenance.migration_not_supported.text')}</p>
              </>
            )}

            {migrationState === MigrationStatus.running && (
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
                  <Spinner className="sw-mb-4" />
                  {progress && <MigrationProgress progress={progress} />}
                </MaintenanceSpinner>
              </>
            )}

            {migrationState === MigrationStatus.succeeded && (
              <>
                <MaintenanceTitle className="text-success">
                  {translate('maintenance.database_is_up_to_date')}
                </MaintenanceTitle>

                <div className="sw-text-center">
                  <LinkStandalone to="/">{translate('layout.home')}</LinkStandalone>
                </div>
              </>
            )}

            {migrationState === MigrationStatus.failed && (
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
