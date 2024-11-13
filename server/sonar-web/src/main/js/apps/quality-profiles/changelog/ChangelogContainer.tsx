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
import { Button } from '@sonarsource/echoes-react';
import { Spinner } from 'design-system';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, Router } from '~sonar-aligned/types/router';
import { ChangelogResponse, getProfileChangelog } from '../../../api/quality-profiles';
import { parseDate, toISO8601WithOffsetString } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { withQualityProfilesContext } from '../qualityProfilesContext';
import { Profile, ProfileChangelogEvent } from '../types';
import { getProfileChangelogPath } from '../utils';
import Changelog from './Changelog';
import ChangelogEmpty from './ChangelogEmpty';
import ChangelogSearch from './ChangelogSearch';

interface Props {
  location: Location;
  profile: Profile;
  router: Router;
  organization: string;
}

interface State {
  events?: ProfileChangelogEvent[];
  loading: boolean;
  page?: number;
  total?: number;
}

class ChangelogContainer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.loadChangelog();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location !== this.props.location) {
      this.loadChangelog();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading() {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  }

  loadChangelog() {
    this.setState({ loading: true });
    const {
      location: { query },
      profile,
    } = this.props;

    getProfileChangelog(query.since, query.to, profile)
      .then((r: ChangelogResponse) => {
        if (this.mounted) {
          this.setState({
            events: r.events,
            total: r.paging.total,
            page: r.paging.pageIndex,
            loading: false,
          });
        }
      })
      .catch(this.stopLoading);
  }

  loadMore(event: React.SyntheticEvent<HTMLElement>) {
    event.preventDefault();
    event.currentTarget.blur();

    if (this.state.page != null) {
      this.setState({ loading: true });
      const {
        location: { query },
        profile,
      } = this.props;

      getProfileChangelog(query.since, query.to, profile, this.state.page + 1)
        .then((r: ChangelogResponse) => {
          if (this.mounted && this.state.events) {
            this.setState(({ events = [] }) => ({
              events: [...events, ...r.events],
              total: r.paging.total,
              page: r.paging.pageIndex,
              loading: false,
            }));
          }
        })
        .catch(this.stopLoading);
    }
  }

  handleDateRangeChange = ({ from, to }: { from?: Date; to?: Date }) => {
    const path = getProfileChangelogPath(this.props.profile.name, this.props.profile.language, this.props.organization, {
      since: from && toISO8601WithOffsetString(from),
      to: to && toISO8601WithOffsetString(to),
    });
    this.props.router.push(path);
  };

  handleReset = () => {
    const path = getProfileChangelogPath(this.props.profile.name, this.props.profile.language, this.props.organization);
    this.props.router.push(path);
  };

  render() {
    const { query } = this.props.location;

    const shouldDisplayFooter =
      this.state.events != null &&
      this.state.total != null &&
      this.state.events.length < this.state.total;

    return (
      <div className="sw-mt-4">
        <div className="sw-mb-2 sw-flex sw-gap-4 sw-items-center">
          <ChangelogSearch
            dateRange={{
              from: query.since ? parseDate(query.since) : undefined,
              to: query.to ? parseDate(query.to) : undefined,
            }}
            onDateRangeChange={this.handleDateRangeChange}
            onReset={this.handleReset}
          />
          <Spinner loading={this.state.loading} />
        </div>

        {this.state.events != null && this.state.events.length === 0 && <ChangelogEmpty />}

        {this.state.events != null && this.state.events.length > 0 && (
          <Changelog events={this.state.events} organization={this.props.organization} />
        )}

        {shouldDisplayFooter && (
          <footer className="sw-text-center sw-mt-2">
            <Button onClick={this.loadMore.bind(this)}>{translate('show_more')}</Button>
          </footer>
        )}
      </div>
    );
  }
}

export default withQualityProfilesContext(withRouter(ChangelogContainer));
