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
import * as PropTypes from 'prop-types';
import ComparisonForm from './ComparisonForm';
import ComparisonResults from './ComparisonResults';
import { compareProfiles } from '../../../api/quality-profiles';
import { getProfileComparePath } from '../utils';
import { Profile } from '../types';

interface Props {
  location: { query: { withKey?: string } };
  organization: string | null;
  profile: Profile;
  profiles: Profile[];
}

type Params = { [p: string]: string };

interface State {
  loading: boolean;
  left?: { name: string };
  right?: { name: string };
  inLeft?: Array<{ key: string; name: string; severity: string }>;
  inRight?: Array<{ key: string; name: string; severity: string }>;
  modified?: Array<{
    key: string;
    name: string;
    left: { params: Params; severity: string };
    right: { params: Params; severity: string };
  }>;
}

export default class ComparisonContainer extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = { loading: false };
  }

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

  loadResults() {
    const { withKey } = this.props.location.query;
    if (!withKey) {
      this.setState({ left: undefined, loading: false });
      return;
    }

    this.setState({ loading: true });
    compareProfiles(this.props.profile.key, withKey).then((r: any) => {
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

  handleCompare = (withKey: string) => {
    const path = getProfileComparePath(
      this.props.profile.name,
      this.props.profile.language,
      this.props.organization,
      withKey
    );
    this.context.router.push(path);
  };

  render() {
    const { profile, profiles, location } = this.props;
    const { withKey } = location.query;
    const { left, right, inLeft, inRight, modified } = this.state;

    return (
      <div className="boxed-group boxed-group-inner js-profile-comparison">
        <header>
          <ComparisonForm
            withKey={withKey}
            profile={profile}
            profiles={profiles}
            onCompare={this.handleCompare}
          />

          {this.state.loading && <i className="spinner spacer-left" />}
        </header>

        {left != null &&
          inLeft != null &&
          right != null &&
          inRight != null &&
          modified != null && (
            <div className="spacer-top">
              <ComparisonResults
                left={left}
                right={right}
                inLeft={inLeft}
                inRight={inRight}
                modified={modified}
                organization={this.props.organization}
              />
            </div>
          )}
      </div>
    );
  }
}
