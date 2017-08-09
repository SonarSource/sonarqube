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
import { connect } from 'react-redux';
import { getComponent } from '../../store/rootReducer';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';

class ProjectAdminContainer extends React.PureComponent {
  /*::
  props: {
    project: {
      configuration?: {
        showSettings: boolean
      }
    }
  };
  */

  componentDidMount() {
    this.checkPermissions();
  }

  componentDidUpdate() {
    this.checkPermissions();
  }

  isProjectAdmin() {
    const { configuration } = this.props.project;
    return configuration != null && configuration.showSettings;
  }

  checkPermissions() {
    if (!this.isProjectAdmin()) {
      handleRequiredAuthorization();
    }
  }

  render() {
    if (!this.isProjectAdmin()) {
      return null;
    }

    return this.props.children;
  }
}

const mapStateToProps = (state, ownProps) => ({
  project: getComponent(state, ownProps.location.query.id)
});

export default connect(mapStateToProps)(ProjectAdminContainer);
