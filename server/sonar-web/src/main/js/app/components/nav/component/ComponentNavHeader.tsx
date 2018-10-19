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
import { Link } from 'react-router';
import ComponentNavBranch from './ComponentNavBranch';
import { Component, Organization, BranchLike, Breadcrumb } from '../../../types';
import QualifierIcon from '../../../../components/icons-components/QualifierIcon';
import {
  getOrganizationByKey,
  areThereCustomOrganizations,
  Store
} from '../../../../store/rootReducer';
import OrganizationAvatar from '../../../../components/common/OrganizationAvatar';
import OrganizationHelmet from '../../../../components/common/OrganizationHelmet';
import OrganizationLink from '../../../../components/ui/OrganizationLink';
import { sanitizeAlmId } from '../../../../helpers/almIntegrations';
import { collapsePath, limitComponentName } from '../../../../helpers/path';
import { getProjectUrl, getBaseUrl } from '../../../../helpers/urls';
import { isSonarCloud } from '../../../../helpers/system';

interface StateProps {
  organization?: Organization;
  shouldOrganizationBeDisplayed?: boolean;
}

interface OwnProps {
  branchLikes: BranchLike[];
  component: Component;
  currentBranchLike: BranchLike | undefined;
  location?: any;
}

interface Props extends StateProps, OwnProps {}

export function ComponentNavHeader(props: Props) {
  const { component, organization, shouldOrganizationBeDisplayed } = props;

  return (
    <header className="navbar-context-header">
      <OrganizationHelmet
        organization={organization && shouldOrganizationBeDisplayed ? organization : undefined}
        title={component.name}
      />
      {organization &&
        shouldOrganizationBeDisplayed && (
          <>
            <OrganizationAvatar organization={organization} />
            <OrganizationLink
              className="navbar-context-header-breadcrumb-link link-base-color link-no-underline spacer-left"
              organization={organization}>
              {organization.name}
            </OrganizationLink>
            <span className="slash-separator" />
          </>
        )}
      {renderBreadcrumbs(component.breadcrumbs)}
      {isSonarCloud() &&
        component.alm && (
          <a
            className="link-no-underline"
            href={component.alm.url}
            rel="noopener noreferrer"
            target="_blank">
            <img
              alt={sanitizeAlmId(component.alm.key)}
              className="text-text-top spacer-left"
              height={16}
              src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(component.alm.key)}.svg`}
              width={16}
            />
          </a>
        )}
      {props.currentBranchLike && (
        <ComponentNavBranch
          branchLikes={props.branchLikes}
          component={component}
          currentBranchLike={props.currentBranchLike}
          // to close dropdown on any location change
          location={props.location}
        />
      )}
    </header>
  );
}

function renderBreadcrumbs(breadcrumbs: Breadcrumb[]) {
  const lastItem = breadcrumbs[breadcrumbs.length - 1];
  return breadcrumbs.map((item, index) => {
    const isPath = item.qualifier === 'DIR';
    const itemName = isPath ? collapsePath(item.name, 15) : limitComponentName(item.name);

    return (
      <React.Fragment key={item.key}>
        {index === 0 && <QualifierIcon className="spacer-right" qualifier={lastItem.qualifier} />}
        <Link
          className="navbar-context-header-breadcrumb-link link-base-color link-no-underline"
          title={item.name}
          to={getProjectUrl(item.key)}>
          {itemName}
        </Link>
        {index < breadcrumbs.length - 1 && <span className="slash-separator" />}
      </React.Fragment>
    );
  });
}

const mapStateToProps = (state: Store, ownProps: OwnProps): StateProps => ({
  organization: getOrganizationByKey(state, ownProps.component.organization),
  shouldOrganizationBeDisplayed: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(ComponentNavHeader);
