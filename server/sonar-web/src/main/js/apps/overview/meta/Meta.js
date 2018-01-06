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
import React from 'react';
import { withRouter } from 'react-router';
import { connect } from 'react-redux';
import MetaKey from './MetaKey';
import MetaOrganizationKey from './MetaOrganizationKey';
import MetaLinks from './MetaLinks';
import MetaQualityGate from './MetaQualityGate';
import MetaQualityProfiles from './MetaQualityProfiles';
import AnalysesList from '../events/AnalysesList';
import MetaSize from './MetaSize';
import MetaTags from './MetaTags';
import { areThereCustomOrganizations } from '../../../store/rootReducer';

const Meta = ({
  branch,
  component,
  history,
  measures,
  areThereCustomOrganizations,
  onComponentChange,
  router
}) => {
  const { qualifier, description, qualityProfiles, qualityGate } = component;

  const isProject = qualifier === 'TRK';

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

      <MetaSize branch={branch} component={component} measures={measures} />

      {isProject && <MetaTags component={component} onComponentChange={onComponentChange} />}

      <AnalysesList
        branch={branch}
        component={component}
        qualifier={component.qualifier}
        history={history}
        router={router}
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
    </div>
  );
};

const mapStateToProps = state => ({
  areThereCustomOrganizations: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(withRouter(Meta));
