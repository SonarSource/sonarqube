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
import MetaSize from './MetaSize';
import MetaTags from './MetaTags';
import BadgesModal from '../badges/BadgesModal';
import AnalysesList from '../events/AnalysesList';
import { Visibility, Component, Metric } from '../../../app/types';
import { History } from '../../../api/time-machine';
import { translate } from '../../../helpers/l10n';
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
          <MetaSize branch={branch} component={component} measures={this.props.measures} />
        </div>

        <AnalysesList
          branch={branch}
          component={component}
          history={this.props.history}
          metrics={metrics}
          qualifier={component.qualifier}
        />

        {isProject && (
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
        )}

        {isProject && <MetaLinks component={component} />}

        <div className="overview-meta-card">
          <MetaKey componentKey={component.key} qualifier={component.qualifier} />
          {organizationsEnabled && <MetaOrganizationKey organization={component.organization} />}
        </div>

        {onSonarCloud &&
          isProject &&
          !isPrivate && <BadgesModal branch={branch} metrics={metrics} project={component.key} />}
      </div>
    );
  }
}
