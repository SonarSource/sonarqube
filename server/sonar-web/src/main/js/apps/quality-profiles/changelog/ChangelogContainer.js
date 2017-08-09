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
// @flow
import React from 'react';
import PropTypes from 'prop-types';
import Changelog from './Changelog';
import ChangelogSearch from './ChangelogSearch';
import ChangelogEmpty from './ChangelogEmpty';
import { getProfileChangelog } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';
import { getProfileChangelogPath } from '../utils';
/*:: import type { Profile } from '../propTypes'; */

/*::
type Props = {
  location: {
    query: {
      since?: string,
      to?: string
    }
  },
  organization: ?string,
  profile: Profile
};
*/

/*::
type State = {
  events?: Array<*>,
  loading: boolean,
  page?: number,
  total?: number
};
*/

export default class ChangelogContainer extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */

  static contextTypes = {
    router: PropTypes.object
  };

  state /*: State */ = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.loadChangelog();
  }

  componentDidUpdate(prevProps /*: Props */) {
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
    const data /*: Object */ = { profileKey: this.props.profile.key };
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

  loadMore(e /*: SyntheticInputEvent */) {
    e.preventDefault();
    e.target.blur();

    this.setState({ loading: true });
    const { query } = this.props.location;
    const data /*: Object */ = {
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
      if (this.mounted && this.state.events) {
        this.setState({
          events: [...this.state.events, ...r.events],
          total: r.total,
          page: r.p,
          loading: false
        });
      }
    });
  }

  handleFromDateChange = (fromDate /*: ?string */) => {
    const path = getProfileChangelogPath(
      this.props.profile.name,
      this.props.profile.language,
      this.props.organization,
      {
        since: fromDate,
        to: this.props.location.query.to
      }
    );
    this.context.router.push(path);
  };

  handleToDateChange = (toDate /*: ?string */) => {
    const path = getProfileChangelogPath(
      this.props.profile.name,
      this.props.profile.language,
      this.props.organization,
      {
        since: this.props.location.query.since,
        to: toDate
      }
    );
    this.context.router.push(path);
  };

  handleReset = () => {
    const path = getProfileChangelogPath(
      this.props.profile.name,
      this.props.profile.language,
      this.props.organization
    );
    this.context.router.push(path);
  };

  render() {
    const { query } = this.props.location;

    const shouldDisplayFooter =
      this.state.events != null &&
      this.state.total != null &&
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
          <Changelog events={this.state.events} organization={this.props.organization} />}

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
