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
import ComponentNav from './nav/component/component-nav';
import { fetchProject } from '../store/rootActions';
import { getComponent } from '../store/rootReducer';

class ProjectContainer extends React.Component {
  static propTypes = {
    project: React.PropTypes.object,
    fetchProject: React.PropTypes.func.isRequired
  };

  componentDidMount () {
    this.props.fetchProject();
  }

  render () {
    if (!this.props.project) {
      return null;
    }

    // FIXME conf
    return (
        <div>
          <ComponentNav component={this.props.project} conf={{}}/>
          {this.props.children}
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  project: getComponent(state, ownProps.location.query.id)
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchProject: () => dispatch(fetchProject(ownProps.location.query.id))
});

export default connect(mapStateToProps, mapDispatchToProps)(ProjectContainer);
