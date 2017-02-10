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
import classNames from 'classnames';
import ProfileInheritanceBox from './ProfileInheritanceBox';
import ChangeParentView from '../views/ChangeParentView';
import { ProfileType } from '../propTypes';
import { translate } from '../../../helpers/l10n';
import { getProfileInheritance } from '../../../api/quality-profiles';

export default class ProfileInheritance extends React.Component {
  static propTypes = {
    profile: ProfileType.isRequired,
    canAdmin: React.PropTypes.bool.isRequired
  };

  state = {
    loading: true
  };

  componentWillMount () {
    this.handleChangeParent = this.handleChangeParent.bind(this);
  }

  componentDidMount () {
    this.mounted = true;
    this.loadData();
  }

  componentDidUpdate (prevProps) {
    if (prevProps.profile !== this.props.profile) {
      this.loadData();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadData () {
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

  handleChangeParent (e) {
    e.preventDefault();
    new ChangeParentView({
      profile: this.props.profile,
      profiles: this.props.profiles
    }).on('done', () => {
      this.props.updateProfiles();
    }).render();
  }

  render () {
    const highlightCurrent = !this.state.loading &&
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
            {this.props.canAdmin && (
                <button
                    className="pull-right js-change-parent"
                    onClick={this.handleChangeParent}>
                  {translate('quality_profiles.change_parent')}
                </button>
            )}
          </header>

          {!this.state.loading && (
              <table className="data zebra">
                <tbody>
                  {this.state.ancestors.map((ancestor, index) => (
                      <ProfileInheritanceBox
                          key={ancestor.key}
                          profile={ancestor}
                          depth={index}
                          className="js-inheritance-ancestor"/>
                  ))}

                  <ProfileInheritanceBox
                      profile={this.state.profile}
                      depth={this.state.ancestors.length}
                      displayLink={false}
                      className={currentClassName}/>

                  {this.state.children.map(child => (
                      <ProfileInheritanceBox
                          key={child.key}
                          profile={child}
                          depth={this.state.ancestors.length + 1}
                          className="js-inheritance-child"/>
                  ))}
                </tbody>
              </table>
          )}
        </div>
    );
  }
}
