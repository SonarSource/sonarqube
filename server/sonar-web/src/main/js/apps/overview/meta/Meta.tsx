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
import MetaKey from './MetaKey';
import MetaOrganizationKey from './MetaOrganizationKey';
import MetaLinks from './MetaLinks';
import MetaQualityGate from './MetaQualityGate';
import MetaQualityProfiles from './MetaQualityProfiles';
import AnalysesList from '../events/AnalysesList';
import MetaSize from './MetaSize';
import MetaTags from './MetaTags';
import BadgesModal from '../badges/BadgesModal';
import { Visibility, Component, Metric } from '../../../app/types';
import { History } from '../../../api/time-machine';
import { MeasureEnhanced } from '../../../helpers/measures';

interface Props {
  branch?: string;
  component: Component;
  history?: History;
  measures: MeasureEnhanced[];
  metrics: { [key: string]: Metric };
  onComponentChange: (changes: {}) => void;
}

export default class Meta extends React.PureComponent<Props> {
  static contextTypes = {
    onSonarCloud: PropTypes.bool,
    organizationsEnabled: PropTypes.bool
  };

  render() {
    const { onSonarCloud, organizationsEnabled } = this.context;
    const { branch, component, metrics } = this.props;
    const { qualifier, description, qualityProfiles, qualityGate, visibility } = component;

    const isProject = qualifier === 'TRK';
    const isPrivate = visibility === Visibility.Private;

    const hasDescription = !!description;
    const hasQualityProfiles = Array.isArray(qualityProfiles) && qualityProfiles.length > 0;
    const hasQualityGate = !!qualityGate;

    const shouldShowQualityProfiles = isProject && hasQualityProfiles;
    const shouldShowQualityGate = isProject && hasQualityGate;
    const hasOrganization = component.organization != null && organizationsEnabled;

    return (
      <div className="overview-meta">
        {hasDescription && (
          <div className="overview-meta-card overview-meta-description">{description}</div>
        )}

        <MetaSize branch={branch} component={component} measures={this.props.measures} />

        {isProject && (
          <MetaTags component={component} onComponentChange={this.props.onComponentChange} />
        )}

        <AnalysesList
          branch={branch}
          component={component}
          history={this.props.history}
          metrics={metrics}
          qualifier={component.qualifier}
        />

        {shouldShowQualityGate && (
          <MetaQualityGate
            gate={qualityGate}
            organization={hasOrganization && component.organization}
          />
        )}

        {shouldShowQualityProfiles && (
          <MetaQualityProfiles
            component={component}
            customOrganizations={organizationsEnabled}
            organization={component.organization}
            profiles={qualityProfiles}
          />
        )}

        {isProject && <MetaLinks component={component} />}

        <MetaKey component={component} />

        {hasOrganization && <MetaOrganizationKey component={component} />}

        {onSonarCloud &&
          isProject &&
          !isPrivate && <BadgesModal branch={branch} metrics={metrics} project={component.key} />}
      </div>
    );
  }
}
