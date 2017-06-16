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
import { connect } from 'react-redux';
import ComponentNav from './nav/component/ComponentNav';
import { fetchProject } from '../../store/rootActions';
import { getComponent } from '../../store/rootReducer';
import { addGlobalErrorMessage } from '../../store/globalMessages/duck';
import { receiveComponents } from '../../store/components/actions';
import { parseError } from '../../apps/code/utils';
import handleRequiredAuthorization from '../utils/handleRequiredAuthorization';

class ProjectContainer extends React.PureComponent {
  props: {
    addGlobalErrorMessage: (message: string) => void,
    children?: React.Element<*>,
    location: {
      query: { id: string }
    },
    project?: {
      configuration: {},
      name: string,
      qualifier: string
    },
    fetchProject: string => Promise<*>,
    receiveComponents: Array<*> => void
  };

  componentDidMount() {
    this.fetchProject();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.location.query.id !== this.props.location.query.id) {
      this.fetchProject();
    }
  }

  fetchProject() {
    this.props.fetchProject(this.props.location.query.id).catch(e => {
      if (e.response && e.response.status === 403) {
        handleRequiredAuthorization();
      } else {
        parseError(e).then(message => this.props.addGlobalErrorMessage(message));
      }
    });
  }

  handleProjectChange = (changes: {}) => {
    this.props.receiveComponents([{ ...this.props.project, ...changes }]);
  };

  render() {
    const { project } = this.props;

    // check `breadcrumbs` to be sure that /api/navigation/component has been already called
    if (!project || project.breadcrumbs == null) {
      return null;
    }

    const isFile = ['FIL', 'UTS'].includes(project.qualifier);
    const configuration = project.configuration || {};

    return (
      <div>
        {!isFile &&
          <ComponentNav component={project} conf={configuration} location={this.props.location} />}
        {/* $FlowFixMe */}
        {React.cloneElement(this.props.children, {
          component: project,
          onComponentChange: this.handleProjectChange
        })}
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  project: getComponent(state, ownProps.location.query.id)
});

const mapDispatchToProps = { addGlobalErrorMessage, fetchProject, receiveComponents };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectContainer);
