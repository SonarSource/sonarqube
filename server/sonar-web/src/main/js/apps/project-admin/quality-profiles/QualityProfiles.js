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
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import Header from './Header';
import Table from './Table';
import { fetchProjectProfiles, setProjectProfile } from '../store/actions';
import {
  areThereCustomOrganizations,
  getProjectAdminAllProfiles,
  getProjectAdminProjectProfiles,
  getComponent
} from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';

type Props = {
  allProfiles: Array<{}>,
  component: { key: string, organization: string },
  customOrganizations: boolean,
  fetchProjectProfiles: (componentKey: string, organization?: string) => void,
  profiles: Array<{}>,
  setProjectProfile: (string, string, string) => void
};

class QualityProfiles extends React.PureComponent {
  props: Props;

  componentDidMount() {
    if (this.props.customOrganizations) {
      this.props.fetchProjectProfiles(this.props.component.key, this.props.component.organization);
    } else {
      this.props.fetchProjectProfiles(this.props.component.key);
    }
  }

  handleChangeProfile = (oldKey, newKey) => {
    this.props.setProjectProfile(this.props.component.key, oldKey, newKey);
  };

  render() {
    const { allProfiles, profiles } = this.props;

    return (
      <div className="page page-limited">
        <Helmet title={translate('project_quality_profiles.page')} />

        <Header />

        {profiles.length > 0
          ? <Table
              allProfiles={allProfiles}
              profiles={profiles}
              onChangeProfile={this.handleChangeProfile}
            />
          : <i className="spinner" />}
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  component: getComponent(state, ownProps.location.query.id),
  customOrganizations: areThereCustomOrganizations(state),
  allProfiles: getProjectAdminAllProfiles(state),
  profiles: getProjectAdminProjectProfiles(state, ownProps.location.query.id)
});

const mapDispatchToProps = { fetchProjectProfiles, setProjectProfile };

export default connect(mapStateToProps, mapDispatchToProps)(QualityProfiles);
