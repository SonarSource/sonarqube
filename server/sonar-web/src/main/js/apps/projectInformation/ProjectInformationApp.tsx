/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Card, LargeCenteredLayout, PageContentFontWrapper, Title } from 'design-system';
import * as React from 'react';
import { getMeasures } from '../../api/measures';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../app/components/available-features/withAvailableFeatures';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import withMetricsContext from '../../app/components/metrics/withMetricsContext';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier, isApplication, isProject } from '../../types/component';
import { Feature } from '../../types/features';
import { MetricKey } from '../../types/metrics';
import { Component, Dict, Measure, Metric } from '../../types/types';
import { CurrentUser, isLoggedIn } from '../../types/users';
import AboutProject from './about/AboutProject';
import ProjectBadges from './badges/ProjectBadges';
import ProjectNotifications from './notifications/ProjectNotifications';
import RegulatoryReport from './projectRegulatoryReport/RegulatoryReport';

interface Props extends WithAvailableFeaturesProps {
  branchLike?: BranchLike;
  component: Component;
  currentUser: CurrentUser;
  onComponentChange: (changes: {}) => void;
  metrics: Dict<Metric>;
}

interface State {
  measures?: Measure[];
}

class ProjectInformationApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.loadMeasures();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

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
    const { measures } = this.state;

    const canConfigureNotifications = isLoggedIn(currentUser) && isProject(component.qualifier);
    const canUseBadges =
      metrics !== undefined &&
      (isApplication(component.qualifier) || isProject(component.qualifier));
    const regulatoryReportFeatureEnabled = this.props.hasFeature(Feature.RegulatoryReport);
    const isApp = isApplication(component.qualifier);

    return (
      <main>
        <LargeCenteredLayout>
          <PageContentFontWrapper>
            <div className="overview sw-my-6 sw-body-sm">
              <Title className="sw-mb-12">
                {translate(isApp ? 'application' : 'project', 'info.title')}
              </Title>
              <div className="sw-grid sw-grid-cols-[488px_minmax(0,_2fr)] sw-gap-x-12 sw-gap-y-3 sw-auto-rows-min">
                <div className="sw-row-span-3">
                  <Card>
                    <AboutProject
                      component={component}
                      measures={measures}
                      onComponentChange={this.props.onComponentChange}
                    />
                  </Card>
                </div>

                {canConfigureNotifications && (
                  <Card>
                    <ProjectNotifications component={component} />
                  </Card>
                )}
                {canUseBadges && (
                  <Card>
                    <ProjectBadges branchLike={branchLike} component={component} />
                  </Card>
                )}
                {component.qualifier === ComponentQualifier.Project &&
                  regulatoryReportFeatureEnabled && (
                    <Card>
                      <RegulatoryReport component={component} branchLike={branchLike} />
                    </Card>
                  )}
              </div>
            </div>
          </PageContentFontWrapper>
        </LargeCenteredLayout>
      </main>
    );
  }
}

export default withComponentContext(
  withCurrentUserContext(withMetricsContext(withAvailableFeatures(ProjectInformationApp)))
);
