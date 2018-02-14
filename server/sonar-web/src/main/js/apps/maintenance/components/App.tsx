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
import * as React from 'react';
import * as classNames from 'classnames';
import { getMigrationStatus, getSystemStatus, migrateDatabase } from '../../../api/system';
import DateFromNow from '../../../components/intl/DateFromNow';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';
import '../styles.css';

interface Props {
  // eslint-disable-next-line camelcase
  location: { query: { return_to?: string } };
  setup: boolean;
}

interface State {
  message?: string;
  startedAt?: string;
  state?: string;
  status?: string;
  wasStarting?: boolean;
}

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
          status: 'OFFLINE'
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
    this.interval = window.setTimeout(this.fetchStatus, 5000);
  };

  loadPreviousPage = () => {
    setInterval(() => {
      window.location.href = this.props.location.query['return_to'] || getBaseUrl() + '/';
    }, 2500);
  };

  handleMigrateClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
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
      <div className="page-wrapper-simple" id="bd">
        <div
          className={classNames('page-simple', { 'panel-warning': state === 'MIGRATION_REQUIRED' })}
          id="nonav">
          {status === 'OFFLINE' && (
            <>
              <h1 className="maintenance-title text-danger">
                {translate('maintenance.sonarqube_is_offline')}
              </h1>
              <p className="maintenance-text">
                {translate('maintenance.sonarqube_is_offline.text')}
              </p>
              <p className="maintenance-text text-center">
                <a href={getBaseUrl() + '/'}>{translate('maintenance.try_again')}</a>
              </p>
            </>
          )}

          {status === 'UP' && (
            <>
              <h1 className="maintenance-title">{translate('maintenance.sonarqube_is_up')}</h1>
              <p className="maintenance-text text-center">
                {translate('maintenance.all_systems_opetational')}
              </p>
              <p className="maintenance-text text-center">
                <a href={getBaseUrl() + '/'}>{translate('layout.home')}</a>
              </p>
            </>
          )}

          {status === 'STARTING' && (
            <>
              <h1 className="maintenance-title">
                {translate('maintenance.sonarqube_is_starting')}
              </h1>
              <p className="maintenance-spinner">
                <i className="spinner" />
              </p>
            </>
          )}

          {status === 'DOWN' && (
            <>
              <h1 className="maintenance-title text-danger">
                {translate('maintenance.sonarqube_is_down')}
              </h1>
              <p className="maintenance-text">{translate('maintenance.sonarqube_is_down.text')}</p>
              <p className="maintenance-text text-center">
                <a href={getBaseUrl() + '/'}>{translate('maintenance.try_again')}</a>
              </p>
            </>
          )}

          {(status === 'DB_MIGRATION_NEEDED' || status === 'DB_MIGRATION_RUNNING') && (
            <>
              <h1 className="maintenance-title">
                {translate('maintenance.sonarqube_is_under_maintenance')}
              </h1>
              <p
                className="maintenance-text"
                dangerouslySetInnerHTML={{
                  __html: translate('maintenance.sonarqube_is_under_maintenance.1')
                }}
              />
              <p
                className="maintenance-text"
                dangerouslySetInnerHTML={{
                  __html: translate('maintenance.sonarqube_is_under_maintenance.2')
                }}
              />
            </>
          )}

          {state === 'NO_MIGRATION' && (
            <>
              <h1 className="maintenance-title">
                {translate('maintenance.database_is_up_to_date')}
              </h1>
              <p className="maintenance-text text-center">
                <a href={getBaseUrl() + '/'}>{translate('layout.home')}</a>
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
                <button id="start-migration" onClick={this.handleMigrateClick} type="button">
                  {translate('maintenance.upgrade')}
                </button>
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
                <a href={getBaseUrl() + '/'}>{translate('layout.home')}</a>
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
    );
  }
}
