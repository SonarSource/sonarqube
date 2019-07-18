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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { Project } from '../types';
import { localizeSorting } from '../utils';
import Coverage from './Coverage';
import Duplications from './Duplications';
import Maintainability from './Maintainability';
import Reliability from './Reliability';
import Risk from './Risk';
import Security from './Security';

interface Props {
  displayOrganizations?: boolean;
  projects: Project[];
  sort?: string;
  total?: number;
  visualization: string;
}

export default class Visualizations extends React.PureComponent<Props> {
  renderVisualization(projects: Project[]) {
    const visualizationToComponent: T.Dict<any> = {
      risk: Risk,
      reliability: Reliability,
      security: Security,
      maintainability: Maintainability,
      coverage: Coverage,
      duplications: Duplications
    };
    const Component = visualizationToComponent[this.props.visualization];

    return Component ? (
      <Component
        displayOrganizations={this.props.displayOrganizations}
        helpText={translate('projects.visualization', this.props.visualization, 'description')}
        projects={projects}
      />
    ) : null;
  }

  renderFooter() {
    const { projects, total, sort } = this.props;

    const limitReached = projects != null && total != null && projects.length < total;

    return limitReached ? (
      <footer className="projects-visualizations-footer">
        <p className="note spacer-top">
          {translateWithParameters(
            'projects.limited_set_of_projects',
            projects!.length,
            localizeSorting(sort)
          )}
        </p>
      </footer>
    ) : null;
  }

  render() {
    const { projects } = this.props;

    return (
      <div className="boxed-group projects-visualizations">
        <div className="projects-visualization">
          {projects != null && this.renderVisualization(projects)}
        </div>
        {this.renderFooter()}
      </div>
    );
  }
}
