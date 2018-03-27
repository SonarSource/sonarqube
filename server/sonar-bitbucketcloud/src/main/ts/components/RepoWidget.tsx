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
import Spinner from '@atlaskit/spinner';
import { getLanguages, Language } from '@sqcore/api/languages';
import ProjectCard from './ProjectCard';
import RepoWidgetNotConfigured from './RepoWidgetNotConfigured';
import { getRepositoryData } from '../api';
import { AppContext, ProjectData } from '../types';

interface Props {
  context: AppContext;
}

interface State {
  languages: Language[];
  loading: boolean;
  project?: ProjectData;
}

export default class RepoWidget extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { languages: [], loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchProjectData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchProjectData = () => {
    Promise.all([getLanguages(), getRepositoryData(this.props.context)]).then(
      ([languages, project]) => {
        if (this.mounted) {
          this.setState({ languages, loading: false, project });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { languages, loading, project } = this.state;

    if (loading) {
      return (
        <div className="widget-padding spacer-top">
          <Spinner size="large" />
        </div>
      );
    }

    return (
      <div className="widget-padding">
        {!project ? (
          <RepoWidgetNotConfigured />
        ) : (
          <ProjectCard languages={languages} project={project} />
        )}
      </div>
    );
  }
}
