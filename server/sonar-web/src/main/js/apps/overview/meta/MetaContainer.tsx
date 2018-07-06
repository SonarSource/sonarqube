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
import * as PropTypes from 'prop-types';
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
import {
  Visibility,
  Component,
  Metric,
  BranchLike,
  CurrentUser,
  Organization
} from '../../../app/types';
import { History } from '../../../api/time-machine';
import { translate } from '../../../helpers/l10n';
import { MeasureEnhanced } from '../../../helpers/measures';
import { hasPrivateAccess } from '../../../helpers/organizations';
import {
  getCurrentUser,
  getMyOrganizations,
  getOrganizationByKey
} from '../../../store/rootReducer';

interface StateToProps {
  currentUser: CurrentUser;
  organization?: Organization;
  userOrganizations: Organization[];
}

interface OwnProps {
  branchLike?: BranchLike;
  component: Component;
  history?: History;
  measures: MeasureEnhanced[];
  metrics: { [key: string]: Metric };
  onComponentChange: (changes: {}) => void;
}

type Props = OwnProps & StateToProps;

export class Meta extends React.PureComponent<Props> {
  static contextTypes = {
    organizationsEnabled: PropTypes.bool
  };

  renderQualityInfos() {
    const { organizationsEnabled } = this.context;
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
      <div className="overview-meta-card">
        {qualityGate && (
          <MetaQualityGate
            organization={organizationsEnabled ? component.organization : undefined}
            qualityGate={qualityGate}
          />
        )}

        {qualityProfiles &&
          qualityProfiles.length > 0 && (
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
    const { organizationsEnabled } = this.context;
    const { branchLike, component, metrics } = this.props;
    const { qualifier, description, visibility } = component;

    const isProject = qualifier === 'TRK';
    const isApp = qualifier === 'APP';
    const isPrivate = visibility === Visibility.Private;

    return (
      <div className="overview-meta">
        <div className="overview-meta-card">
          <h4 className="overview-meta-header">
            {translate('overview.about_this_project', qualifier)}
          </h4>
          {description !== undefined && <p className="overview-meta-description">{description}</p>}
          {isProject && (
            <MetaTags component={component} onComponentChange={this.props.onComponentChange} />
          )}
          <MetaSize branchLike={branchLike} component={component} measures={this.props.measures} />
        </div>

        <AnalysesList
          branchLike={branchLike}
          component={component}
          history={this.props.history}
          metrics={metrics}
          qualifier={component.qualifier}
        />

        {this.renderQualityInfos()}

        {isProject && <MetaLinks component={component} />}

        <div className="overview-meta-card">
          <MetaKey componentKey={component.key} qualifier={component.qualifier} />
          {organizationsEnabled && <MetaOrganizationKey organization={component.organization} />}
        </div>

        {!isPrivate &&
          (isProject || isApp) && (
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

const mapStateToProps = (state: any, { component }: OwnProps) => ({
  currentUser: getCurrentUser(state),
  organization: getOrganizationByKey(state, component.organization),
  userOrganizations: getMyOrganizations(state)
});

export default connect<StateToProps, {}, OwnProps>(mapStateToProps)(Meta);
