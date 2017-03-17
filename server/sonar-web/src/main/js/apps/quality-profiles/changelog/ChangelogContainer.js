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
import React from 'react';
import Changelog from './Changelog';
import ChangelogSearch from './ChangelogSearch';
import ChangelogEmpty from './ChangelogEmpty';
import { getProfileChangelog } from '../../../api/quality-profiles';
import { ProfileType } from '../propTypes';
import { translate } from '../../../helpers/l10n';

export default class ChangelogContainer extends React.Component {
  static propTypes = {
    location: React.PropTypes.object.isRequired,
    profile: ProfileType
  };

  static contextTypes = {
    router: React.PropTypes.object
  };

  state = {
    loading: true
  };

  componentWillMount() {
    this.handleFromDateChange = this.handleFromDateChange.bind(this);
    this.handleToDateChange = this.handleToDateChange.bind(this);
    this.handleReset = this.handleReset.bind(this);
  }

  componentDidMount() {
    this.mounted = true;
    this.loadChangelog();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.location !== this.props.location) {
      this.loadChangelog();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadChangelog() {
    this.setState({ loading: true });
    const { query } = this.props.location;
    const data = { profileKey: this.props.profile.key };
    if (query.since) {
      data.since = query.since;
    }
    if (query.to) {
      data.to = query.to;
    }

    getProfileChangelog(data).then(r => {
      if (this.mounted) {
        this.setState({
          events: r.events,
          total: r.total,
          page: r.p,
          loading: false
        });
      }
    });
  }

  loadMore(e) {
    e.preventDefault();
    e.target.blur();

    this.setState({ loading: true });
    const { query } = this.props.location;
    const data = {
      profileKey: this.props.profile.key,
      p: this.state.page + 1
    };
    if (query.since) {
      data.since = query.since;
    }
    if (query.to) {
      data.to = query.to;
    }

    getProfileChangelog(data).then(r => {
      if (this.mounted) {
        this.setState({
          events: [...this.state.events, ...r.events],
          total: r.total,
          page: r.p,
          loading: false
        });
      }
    });
  }

  handleFromDateChange(fromDate) {
    const query = { ...this.props.location.query, since: fromDate };
    this.context.router.push({ pathname: '/profiles/changelog', query });
  }

  handleToDateChange(toDate) {
    const query = { ...this.props.location.query, to: toDate };
    this.context.router.push({ pathname: '/profiles/changelog', query });
  }

  handleReset() {
    const query = { key: this.props.profile.key };
    this.context.router.push({ pathname: '/profiles/changelog', query });
  }

  render() {
    const { query } = this.props.location;

    const shouldDisplayFooter = this.state.events != null &&
      this.state.events.length < this.state.total;

    return (
      <div className="quality-profile-box js-profile-changelog">
        <header className="spacer-bottom">
          <ChangelogSearch
            fromDate={query.since}
            toDate={query.to}
            onFromDateChange={this.handleFromDateChange}
            onToDateChange={this.handleToDateChange}
            onReset={this.handleReset}
          />

          {this.state.loading && <i className="spinner spacer-left" />}
        </header>

        {this.state.events != null && this.state.events.length === 0 && <ChangelogEmpty />}

        {this.state.events != null &&
          this.state.events.length > 0 &&
          <Changelog events={this.state.events} />}

        {shouldDisplayFooter &&
          <footer className="text-center spacer-top small">
            <a href="#" onClick={this.loadMore.bind(this)}>
              {translate('show_more')}
            </a>
          </footer>}
      </div>
    );
  }
}
