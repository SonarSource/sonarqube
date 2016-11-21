/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { connect } from 'react-redux';
import shallowCompare from 'react-addons-shallow-compare';
import Header from './Header';
import Table from './Table';
import GlobalMessagesContainer from '../components/GlobalMessagesContainer';
import { fetchProjectProfiles, setProjectProfile } from '../store/actions';
import { getProjectAdminAllProfiles, getProjectAdminProjectProfiles } from '../../../app/store/rootReducer';

class QualityProfiles extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired,
    allProfiles: React.PropTypes.array,
    profiles: React.PropTypes.array
  };

  componentDidMount () {
    this.props.fetchProjectProfiles(this.props.component.key);
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleChangeProfile (oldKey, newKey) {
    this.props.setProjectProfile(this.props.component.key, oldKey, newKey);
  }

  render () {
    const { allProfiles, profiles } = this.props;

    return (
        <div className="page page-limited">
          <Header/>

          <GlobalMessagesContainer/>

          {profiles.length > 0 ? (
              <Table
                  allProfiles={allProfiles}
                  profiles={profiles}
                  onChangeProfile={this.handleChangeProfile.bind(this)}/>
          ) : (
              <i className="spinner"/>
          )}
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  allProfiles: getProjectAdminAllProfiles(state),
  profiles: getProjectAdminProjectProfiles(state, ownProps.component.key)
});

export default connect(
    mapStateToProps,
    { fetchProjectProfiles, setProjectProfile }
)(QualityProfiles);
