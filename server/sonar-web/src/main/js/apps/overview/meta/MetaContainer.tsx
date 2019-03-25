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
import { connect } from 'react-redux';
import MetaKey from './MetaKey';
import MetaOrganizationKey from './MetaOrganizationKey';
import MetaLinks from './MetaLinks';
import MetaQualityGate from './MetaQualityGate';
import MetaQualityProfiles from './MetaQualityProfiles';
import MetaSize from './MetaSize';
import MetaTags from './MetaTags';
import BadgesModal from '../badges/BadgesModal';
import AnalysesList from '../events/AnalysesList';
import { translate } from '../../../helpers/l10n';
import { hasPrivateAccess } from '../../../helpers/organizations';
import {
  getCurrentUser,
  getMyOrganizations,
  getOrganizationByKey,
  Store,
  getAppState
} from '../../../store/rootReducer';
import PrivacyBadgeContainer from '../../../components/common/PrivacyBadgeContainer';

interface StateToProps {
  appState: T.AppState;
  currentUser: T.CurrentUser;
  organization?: T.Organization;
  userOrganizations: T.Organization[];
}

interface OwnProps {
  branchLike?: T.BranchLike;
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
    const { branchLike, component, measures, metrics, organization } = this.props;
    const { qualifier, description, visibility } = component;

    const isProject = qualifier === 'TRK';
    const isApp = qualifier === 'APP';
    const isPrivate = visibility === 'private';
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

        {!isPrivate && (isProject || isApp) && metrics && (
          <BadgesModal
            branchLike={branchLike}
            metrics={metrics}
            project={component.key}
            qualifier={component.qualifier}
          />
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
