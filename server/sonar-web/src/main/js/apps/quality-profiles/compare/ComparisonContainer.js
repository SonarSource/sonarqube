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
import ComparisonForm from './ComparisonForm';
import ComparisonResults from './ComparisonResults';
import { ProfileType, ProfilesListType } from '../propTypes';
import { compareProfiles } from '../../../api/quality-profiles';

export default class ComparisonContainer extends React.Component {
  static propTypes = {
    profile: ProfileType,
    profiles: ProfilesListType
  };

  static contextTypes = {
    router: React.PropTypes.object
  };

  state = {
    loading: false
  };

  componentWillMount () {
    this.handleCompare = this.handleCompare.bind(this);
  }

  componentDidMount () {
    this.mounted = true;
    this.loadResults();
  }

  componentDidUpdate (prevProps) {
    if (prevProps.profile !== this.props.profile ||
        prevProps.location !== this.props.location) {
      this.loadResults();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadResults () {
    const { withKey } = this.props.location.query;
    if (!withKey) {
      this.setState({ left: null, loading: false });
      return;
    }

    this.setState({ loading: true });
    compareProfiles(this.props.profile.key, withKey).then(r => {
      if (this.mounted) {
        this.setState({
          left: r.left,
          right: r.right,
          inLeft: r.inLeft,
          inRight: r.inRight,
          modified: r.modified,
          loading: false
        });
      }
    });
  }

  handleCompare (withKey) {
    this.context.router.push({
      pathname: '/profiles/compare',
      query: {
        key: this.props.profile.key,
        withKey
      }
    });
  }

  render () {
    const { profile, profiles, location } = this.props;
    const { withKey } = location.query;
    const { left, right, inLeft, inRight, modified } = this.state;

    return (
        <div className="quality-profile-box js-profile-comparison">
          <header className="spacer-bottom">
            <ComparisonForm
                withKey={withKey}
                profile={profile}
                profiles={profiles}
                onCompare={this.handleCompare}/>

            {this.state.loading && (
                <i className="spinner spacer-left"/>
            )}
          </header>

          {left != null && (
              <ComparisonResults
                  left={left}
                  right={right}
                  inLeft={inLeft}
                  inRight={inRight}
                  modified={modified}/>
          )}
        </div>
    );
  }
}
