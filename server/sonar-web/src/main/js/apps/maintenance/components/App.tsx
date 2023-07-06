/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { getMigrationStatus, getSystemStatus, migrateDatabase } from '../../../api/system';
import InstanceMessage from '../../../components/common/InstanceMessage';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import DateFromNow from '../../../components/intl/DateFromNow';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { getReturnUrl } from '../../../helpers/urls';
import '../styles.css';

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
    if (this.interval) {
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
      () => {}
    );
  };

  render() {
    const { state, status } = this.state;

    return (
      <>
        <Helmet defaultTitle={translate('maintenance.page')} defer={false} />
        <div className="page-wrapper-simple" id="bd">
          <div
            className={classNames('page-simple', {
              'panel-warning': state === 'MIGRATION_REQUIRED',
            })}
            id="nonav"
          >
            {status === 'OFFLINE' && (
              <>
                <h1 className="maintenance-title text-danger">
                  <InstanceMessage message={translate('maintenance.is_offline')} />
                </h1>
                <p className="maintenance-text">
                  {translate('maintenance.sonarqube_is_offline.text')}
                </p>
                <p className="maintenance-text text-center">
                  {/* We don't use <Link> here, as we want to fully refresh the page. */}
                  <a href={getBaseUrl() + '/'}>{translate('maintenance.try_again')}</a>
                </p>
              </>
            )}

            {status === 'UP' && (
              <>
                <h1 className="maintenance-title">
                  <InstanceMessage message={translate('maintenance.is_up')} />
                </h1>
                <p className="maintenance-text text-center">
                  {translate('maintenance.all_systems_opetational')}
                </p>
                <p className="maintenance-text text-center">
                  <Link to="/">{translate('layout.home')}</Link>
                </p>
              </>
            )}

            {status === 'STARTING' && (
              <>
                <h1 className="maintenance-title">
                  <InstanceMessage message={translate('maintenance.is_starting')} />
                </h1>
                <p className="maintenance-spinner">
                  <i className="spinner" />
                </p>
              </>
            )}

            {status === 'DOWN' && (
              <>
                <h1 className="maintenance-title text-danger">
                  <InstanceMessage message={translate('maintenance.is_down')} />
                </h1>
                <p className="maintenance-text">
                  {translate('maintenance.sonarqube_is_down.text')}
                </p>
                <p className="maintenance-text text-center">
                  {/* We don't use <Link> here, as we want to fully refresh the page. */}
                  <a href={getBaseUrl() + '/'}>{translate('maintenance.try_again')}</a>
                </p>
              </>
            )}

            {(status === 'DB_MIGRATION_NEEDED' || status === 'DB_MIGRATION_RUNNING') && (
              <>
                <h1 className="maintenance-title">
                  <InstanceMessage message={translate('maintenance.is_under_maintenance')} />
                </h1>
                <p className="maintenance-text">
                  <FormattedMessage
                    defaultMessage={translate('maintenance.sonarqube_is_under_maintenance.1')}
                    id="maintenance.sonarqube_is_under_maintenance.1"
                    values={{
                      link: (
                        <Link
                          to="https://www.sonarlint.org/?referrer=sonarqube-maintenance"
                          target="_blank"
                        >
                          {translate('maintenance.sonarqube_is_under_maintenance_link.1')}
                        </Link>
                      ),
                    }}
                  />
                </p>
                <p className="maintenance-text">
                  <FormattedMessage
                    defaultMessage={translate('maintenance.sonarqube_is_under_maintenance.2')}
                    id="maintenance.sonarqube_is_under_maintenance.2"
                    values={{
                      link: (
                        <Link
                          // We cannot use <DocLink> here, as it relies on AppState. However, the maintenance
                          // app is a special app that can run in a "downgraded" environment, where the AppState
                          // may not yet be fully loaded. Hence, we link to this documentation page directly.
                          to="https://knowledgebase.autorabit.com/codescan/docs/codescan-self-hosted"
                          target="_blank"
                        >
                          {translate('maintenance.sonarqube_is_under_maintenance_link.2')}
                        </Link>
                      ),
                    }}
                  />
                </p>
              </>
            )}

            {state === 'NO_MIGRATION' && (
              <>
                <h1 className="maintenance-title">
                  {translate('maintenance.database_is_up_to_date')}
                </h1>
                <p className="maintenance-text text-center">
                  <Link to="/">{translate('layout.home')}</Link>
                </p>
              </>
            )}

            {state === 'MIGRATION_REQUIRED' && (
              <>
                <h1 className="maintenance-title">{translate('maintenance.upgrade_database')}</h1>
                <p className="maintenance-text">{translate('maintenance.upgrade_database.1')}</p>
                <p className="maintenance-text">{translate('maintenance.upgrade_database.2')}</p>
                <p className="maintenance-text">{translate('maintenance.upgrade_database.3')}</p>
                <div className="maintenance-spinner">
                  <Button id="start-migration" onClick={this.handleMigrateClick}>
                    {translate('maintenance.upgrade')}
                  </Button>
                </div>
              </>
            )}

            {state === 'NOT_SUPPORTED' && (
              <>
                <h1 className="maintenance-title text-danger">
                  {translate('maintenance.migration_not_supported')}
                </h1>
                <p>{translate('maintenance.migration_not_supported.text')}</p>
              </>
            )}

            {state === 'MIGRATION_RUNNING' && (
              <>
                <h1 className="maintenance-title">{translate('maintenance.database_migration')}</h1>
                {this.state.message && (
                  <p className="maintenance-text text-center">{this.state.message}</p>
                )}
                {this.state.startedAt && (
                  <p className="maintenance-text text-center">
                    {translate('background_tasks.table.started')}{' '}
                    <DateFromNow date={this.state.startedAt} />
                    <br />
                    <small className="text-muted">
                      <TimeFormatter date={this.state.startedAt} long={true} />
                    </small>
                  </p>
                )}
                <p className="maintenance-spinner">
                  <i className="spinner" />
                </p>
              </>
            )}

            {state === 'MIGRATION_SUCCEEDED' && (
              <>
                <h1 className="maintenance-title text-success">
                  {translate('maintenance.database_is_up_to_date')}
                </h1>
                <p className="maintenance-text text-center">
                  <Link to="/">{translate('layout.home')}</Link>
                </p>
              </>
            )}

            {state === 'MIGRATION_FAILED' && (
              <>
                <h1 className="maintenance-title text-danger">
                  {translate('maintenance.upgrade_failed')}
                </h1>
                <p className="maintenance-text">{translate('maintenance.upgrade_failed.text')}</p>
              </>
            )}
          </div>
        </div>
      </>
    );
  }
}
