/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { getMeasures } from '../../../../../api/measures';
import { BranchLike } from '../../../../../types/branch-like';
import { ComponentQualifier } from '../../../../../types/component';
import { MetricKey } from '../../../../../types/metrics';
import { Component, Dict, Measure, Metric } from '../../../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../../../types/users';
import withCurrentUserContext from '../../../current-user/withCurrentUserContext';
import withMetricsContext from '../../../metrics/withMetricsContext';
import ProjectBadges from './badges/ProjectBadges';
import InfoDrawerPage from './InfoDrawerPage';
import ProjectNotifications from './notifications/ProjectNotifications';
import './ProjectInformation.css';
import { ProjectInformationPages } from './ProjectInformationPages';
import ProjectInformationRenderer from './ProjectInformationRenderer';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  currentUser: CurrentUser;
  onComponentChange: (changes: {}) => void;
  metrics: Dict<Metric>;
}

interface State {
  measures?: Measure[];
  page: ProjectInformationPages;
}

export class ProjectInformation extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    page: ProjectInformationPages.main,
  };

  componentDidMount() {
    this.mounted = true;
    this.loadMeasures();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  setPage = (page: ProjectInformationPages = ProjectInformationPages.main) => {
    this.setState({ page });
  };

  loadMeasures = () => {
    const {
      component: { key },
    } = this.props;

    return getMeasures({
      component: key,
      metricKeys: [MetricKey.ncloc, MetricKey.projects].join(),
    }).then((measures) => {
      if (this.mounted) {
        this.setState({ measures });
      }
    });
  };

  render() {
    const { branchLike, component, currentUser, metrics } = this.props;
    const { measures, page } = this.state;

    const canConfigureNotifications =
      isLoggedIn(currentUser) && component.qualifier === ComponentQualifier.Project;
    const canUseBadges =
      metrics !== undefined &&
      (component.qualifier === ComponentQualifier.Application ||
        component.qualifier === ComponentQualifier.Project);

    return (
      <>
        <ProjectInformationRenderer
          canConfigureNotifications={canConfigureNotifications}
          canUseBadges={canUseBadges}
          component={component}
          branchLike={branchLike}
          measures={measures}
          onComponentChange={this.props.onComponentChange}
          onPageChange={this.setPage}
        />
        {canUseBadges && (
          <InfoDrawerPage
            displayed={page === ProjectInformationPages.badges}
            onPageChange={this.setPage}
          >
            <ProjectBadges branchLike={branchLike} component={component} />
          </InfoDrawerPage>
        )}
        {canConfigureNotifications && (
          <InfoDrawerPage
            displayed={page === ProjectInformationPages.notifications}
            onPageChange={this.setPage}
          >
            <ProjectNotifications component={component} />
          </InfoDrawerPage>
        )}
      </>
    );
  }
}

export default withCurrentUserContext(withMetricsContext(ProjectInformation));
