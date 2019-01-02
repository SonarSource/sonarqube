/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { withRouter, WithRouterProps } from 'react-router';
import ComparisonForm from './ComparisonForm';
import ComparisonResults from './ComparisonResults';
import { compareProfiles, CompareResponse } from '../../../api/quality-profiles';
import { getProfileComparePath } from '../utils';
import { Profile } from '../types';

interface Props extends WithRouterProps {
  organization?: string;
  profile: Profile;
  profiles: Profile[];
}

type State = { loading: boolean } & Partial<CompareResponse>;
type StateWithResults = { loading: boolean } & CompareResponse;

class ComparisonContainer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
    this.loadResults();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.profile !== this.props.profile || prevProps.location !== this.props.location) {
      this.loadResults();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadResults = () => {
    const { withKey } = this.props.location.query;
    if (!withKey) {
      this.setState({ left: undefined, loading: false });
      return Promise.resolve();
    }

    this.setState({ loading: true });
    return compareProfiles(this.props.profile.key, withKey).then(
      ({ left, right, inLeft, inRight, modified }) => {
        if (this.mounted) {
          this.setState({ left, right, inLeft, inRight, modified, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleCompare = (withKey: string) => {
    const path = getProfileComparePath(
      this.props.profile.name,
      this.props.profile.language,
      this.props.organization,
      withKey
    );
    this.props.router.push(path);
  };

  hasResults(state: State): state is StateWithResults {
    return state.left !== undefined;
  }

  render() {
    const { profile, profiles, location } = this.props;
    const { withKey } = location.query;

    return (
      <div className="boxed-group boxed-group-inner js-profile-comparison">
        <header>
          <ComparisonForm
            onCompare={this.handleCompare}
            profile={profile}
            profiles={profiles}
            withKey={withKey}
          />

          {this.state.loading && <i className="spinner spacer-left" />}
        </header>

        {this.hasResults(this.state) && (
          <div className="spacer-top">
            <ComparisonResults
              inLeft={this.state.inLeft}
              inRight={this.state.inRight}
              left={this.state.left}
              leftProfile={profile}
              modified={this.state.modified}
              organization={this.props.organization}
              refresh={this.loadResults}
              right={this.state.right}
              rightProfile={profiles.find(p => p.key === withKey)}
            />
          </div>
        )}
      </div>
    );
  }
}

export default withRouter(ComparisonContainer);
