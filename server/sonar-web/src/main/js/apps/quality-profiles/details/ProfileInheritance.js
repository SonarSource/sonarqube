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
import classNames from 'classnames';
import ProfileInheritanceBox from './ProfileInheritanceBox';
import ChangeParentForm from './ChangeParentForm';
import { translate } from '../../../helpers/l10n';
import { getProfileInheritance } from '../../../api/quality-profiles';
import type { Profile } from '../propTypes';

type Props = {
  canAdmin: boolean,
  onRequestFail: Object => void,
  organization: ?string,
  profile: Profile,
  profiles: Array<Profile>,
  updateProfiles: () => Promise<*>
};

type ProfileInheritanceDetails = {
  activeRuleCount: number,
  key: string,
  language: string,
  name: string,
  overridingRuleCount?: number
};

type State = {
  ancestors?: Array<ProfileInheritanceDetails>,
  children?: Array<ProfileInheritanceDetails>,
  formOpen: boolean,
  loading: boolean,
  profile?: ProfileInheritanceDetails
};

export default class ProfileInheritance extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
    formOpen: false,
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.loadData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.profile !== this.props.profile) {
      this.loadData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadData() {
    getProfileInheritance(this.props.profile.key).then(r => {
      if (this.mounted) {
        const { ancestors, children } = r;
        this.setState({
          children,
          ancestors: ancestors.reverse(),
          profile: r.profile,
          loading: false
        });
      }
    });
  }

  handleChangeParentClick = (event: Event) => {
    event.preventDefault();
    this.setState({ formOpen: true });
  };

  closeForm = () => {
    this.setState({ formOpen: false });
  };

  handleParentChange = () => {
    this.props.updateProfiles();
    this.closeForm();
  };

  render() {
    const { profile, profiles } = this.props;

    const highlightCurrent =
      !this.state.loading &&
      this.state.ancestors != null &&
      this.state.children != null &&
      (this.state.ancestors.length > 0 || this.state.children.length > 0);
    const currentClassName = classNames('js-inheritance-current', {
      selected: highlightCurrent
    });

    return (
      <div className="quality-profile-inheritance">
        <header className="big-spacer-bottom clearfix">
          <h2 className="pull-left">
            {translate('quality_profiles.profile_inheritance')}
          </h2>
          {this.props.canAdmin &&
            <button className="pull-right js-change-parent" onClick={this.handleChangeParentClick}>
              {translate('quality_profiles.change_parent')}
            </button>}
        </header>

        {!this.state.loading &&
          <table className="data zebra">
            <tbody>
              {this.state.ancestors != null &&
                this.state.ancestors.map((ancestor, index) => (
                  <ProfileInheritanceBox
                    className="js-inheritance-ancestor"
                    depth={index}
                    key={ancestor.key}
                    language={profile.language}
                    organization={this.props.organization}
                    profile={ancestor}
                  />
                ))}

              <ProfileInheritanceBox
                className={currentClassName}
                depth={this.state.ancestors ? this.state.ancestors.length : 0}
                displayLink={false}
                language={profile.language}
                organization={this.props.organization}
                profile={this.state.profile}
              />

              {this.state.children != null &&
                this.state.children.map(child => (
                  <ProfileInheritanceBox
                    className="js-inheritance-child"
                    depth={this.state.ancestors ? this.state.ancestors.length + 1 : 0}
                    key={child.key}
                    language={profile.language}
                    organization={this.props.organization}
                    profile={child}
                  />
                ))}
            </tbody>
          </table>}

        {this.state.formOpen &&
          <ChangeParentForm
            onChange={this.handleParentChange}
            onClose={this.closeForm}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
            profiles={profiles.filter(p => p !== profile && p.language === profile.language)}
          />}
      </div>
    );
  }
}
