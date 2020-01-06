/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { connect } from 'react-redux';
import { lazyLoadComponent } from 'sonar-ui-common/components/lazyLoadComponent';
import { translate } from 'sonar-ui-common/helpers/l10n';
import PrivacyBadgeContainer from '../../../components/common/PrivacyBadgeContainer';
import { hasPrivateAccess } from '../../../helpers/organizations';
import { isLoggedIn } from '../../../helpers/users';
import {
  getAppState,
  getCurrentUser,
  getMyOrganizations,
  getOrganizationByKey,
  Store
} from '../../../store/rootReducer';
import { BranchLike } from '../../../types/branch-like';
import AnalysesList from '../events/AnalysesList';
import MetaKey from './MetaKey';
import MetaLinks from './MetaLinks';
import MetaOrganizationKey from './MetaOrganizationKey';
import MetaQualityGate from './MetaQualityGate';
import MetaQualityProfiles from './MetaQualityProfiles';
import MetaSize from './MetaSize';
import MetaTags from './MetaTags';

const ProjectBadges = lazyLoadComponent(() => import('../badges/ProjectBadges'), 'ProjectBadges');
const ProjectNotifications = lazyLoadComponent(
  () => import('../notifications/ProjectNotifications'),
  'ProjectNotifications'
);

interface StateToProps {
  appState: T.AppState;
  currentUser: T.CurrentUser;
  organization?: T.Organization;
  userOrganizations: T.Organization[];
}

interface OwnProps {
  branchLike?: BranchLike;
  component: T.Component;
  history?: {
    [metric: string]: Array<{ date: Date; value?: string }>;
  };
  measures?: T.MeasureEnhanced[];
  metrics?: T.Dict<T.Metric>;
  onComponentChange: (changes: {}) => void;
}

type Props = OwnProps & StateToProps;

export class Meta extends React.PureComponent<Props> {
  renderQualityInfos() {
    const { organizationsEnabled } = this.props.appState;
    const { component, currentUser, organization, userOrganizations } = this.props;
    const { qualifier, qualityProfiles, qualityGate } = component;
    const isProject = qualifier === 'TRK';

    if (
      !isProject ||
      (organizationsEnabled && !hasPrivateAccess(currentUser, organization, userOrganizations))
    ) {
      return null;
    }

    return (
      <div className="overview-meta-card" id="overview-meta-quality-gate">
        {qualityGate && (
          <MetaQualityGate
            organization={organizationsEnabled ? component.organization : undefined}
            qualityGate={qualityGate}
          />
        )}

        {qualityProfiles && qualityProfiles.length > 0 && (
          <MetaQualityProfiles
            headerClassName={qualityGate ? 'big-spacer-top' : undefined}
            organization={organizationsEnabled ? component.organization : undefined}
            profiles={qualityProfiles}
          />
        )}
      </div>
    );
  }

  render() {
    const { organizationsEnabled } = this.props.appState;
    const { branchLike, component, currentUser, measures, metrics, organization } = this.props;
    const { qualifier, description, visibility } = component;

    const isProject = qualifier === 'TRK';
    const isApp = qualifier === 'APP';
    const isPrivate = visibility === 'private';
    const canUseBadges = !isPrivate && (isProject || isApp);
    const canConfigureNotifications = isLoggedIn(currentUser);

    return (
      <div className="overview-meta">
        <div className="overview-meta-card">
          <h4 className="overview-meta-header">
            {translate('overview.about_this_project', qualifier)}
            {component.visibility && (
              <PrivacyBadgeContainer
                className="spacer-left pull-right"
                organization={organization}
                qualifier={component.qualifier}
                tooltipProps={{ projectKey: component.key }}
                visibility={component.visibility}
              />
            )}
          </h4>
          {description !== undefined && <p className="overview-meta-description">{description}</p>}
          {isProject && (
            <MetaTags component={component} onComponentChange={this.props.onComponentChange} />
          )}
          {measures && (
            <MetaSize branchLike={branchLike} component={component} measures={measures} />
          )}
        </div>

        {metrics && (
          <AnalysesList
            branchLike={branchLike}
            component={component}
            history={this.props.history}
            metrics={metrics}
            qualifier={component.qualifier}
          />
        )}

        {this.renderQualityInfos()}

        {isProject && <MetaLinks component={component} />}

        <div className="overview-meta-card">
          <MetaKey componentKey={component.key} qualifier={component.qualifier} />
          {organizationsEnabled && <MetaOrganizationKey organization={component.organization} />}
        </div>

        {(canUseBadges || canConfigureNotifications) && (
          <div className="overview-meta-card">
            {canUseBadges && metrics !== undefined && (
              <ProjectBadges
                branchLike={branchLike}
                metrics={metrics}
                project={component.key}
                qualifier={component.qualifier}
              />
            )}

            {canConfigureNotifications && (
              <ProjectNotifications className="spacer-top spacer-bottom" component={component} />
            )}
          </div>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state: Store, { component }: OwnProps) => ({
  appState: getAppState(state),
  currentUser: getCurrentUser(state),
  organization: getOrganizationByKey(state, component.organization),
  userOrganizations: getMyOrganizations(state)
});

export default connect(mapStateToProps)(Meta);
