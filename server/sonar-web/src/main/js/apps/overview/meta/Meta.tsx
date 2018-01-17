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
import { connect } from 'react-redux';
import MetaKey from './MetaKey';
import MetaOrganizationKey from './MetaOrganizationKey';
import MetaLinks from './MetaLinks';
import MetaQualityGate from './MetaQualityGate';
import MetaQualityProfiles from './MetaQualityProfiles';
import AnalysesList from '../events/AnalysesList';
import MetaSize from './MetaSize';
import MetaTags from './MetaTags';
import BadgesModal from '../badges/BadgesModal';
import { areThereCustomOrganizations, getGlobalSettingValue } from '../../../store/rootReducer';
import { Visibility, Component } from '../../../app/types';
import { History } from '../../../api/time-machine';
import { MeasureEnhanced } from '../../../helpers/measures';

interface OwnProps {
  branch?: string;
  component: Component;
  history?: History;
  measures: MeasureEnhanced[];
  onComponentChange: (changes: {}) => void;
}

interface StateToProps {
  areThereCustomOrganizations: boolean;
  onSonarCloud: boolean;
}

export function Meta(props: OwnProps & StateToProps) {
  const { branch, component, areThereCustomOrganizations } = props;
  const { qualifier, description, qualityProfiles, qualityGate, visibility } = component;

  const isProject = qualifier === 'TRK';
  const isPrivate = visibility === Visibility.Private;

  const hasDescription = !!description;
  const hasQualityProfiles = Array.isArray(qualityProfiles) && qualityProfiles.length > 0;
  const hasQualityGate = !!qualityGate;

  const shouldShowQualityProfiles = isProject && hasQualityProfiles;
  const shouldShowQualityGate = isProject && hasQualityGate;
  const hasOrganization = component.organization != null && areThereCustomOrganizations;

  return (
    <div className="overview-meta">
      {hasDescription && (
        <div className="overview-meta-card overview-meta-description">{description}</div>
      )}

      <MetaSize branch={branch} component={component} measures={props.measures} />

      {isProject && <MetaTags component={component} onComponentChange={props.onComponentChange} />}

      <AnalysesList
        branch={branch}
        component={component}
        qualifier={component.qualifier}
        history={props.history}
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
          customOrganizations={areThereCustomOrganizations}
          profiles={qualityProfiles}
        />
      )}

      {isProject && <MetaLinks component={component} />}

      <MetaKey component={component} />

      {hasOrganization && <MetaOrganizationKey component={component} />}

      {props.onSonarCloud &&
        isProject &&
        !isPrivate && <BadgesModal branch={branch} project={component.key} />}
    </div>
  );
}

const mapStateToProps = (state: any): StateToProps => {
  const sonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');
  return {
    areThereCustomOrganizations: areThereCustomOrganizations(state),
    onSonarCloud: Boolean(sonarCloudSetting && sonarCloudSetting.value === 'true')
  };
};

export default connect<StateToProps, {}, OwnProps>(mapStateToProps)(Meta);
