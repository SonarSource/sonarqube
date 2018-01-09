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
import { Component, Organization, Branch, Breadcrumb } from '../../../types';
import QualifierIcon from '../../../../components/shared/QualifierIcon';
import { getOrganizationByKey, areThereCustomOrganizations } from '../../../../store/rootReducer';
import OrganizationAvatar from '../../../../components/common/OrganizationAvatar';
import OrganizationHelmet from '../../../../components/common/OrganizationHelmet';
import OrganizationLink from '../../../../components/ui/OrganizationLink';
import PrivateBadge from '../../../../components/common/PrivateBadge';
import { collapsePath, limitComponentName } from '../../../../helpers/path';
import { getProjectUrl } from '../../../../helpers/urls';

interface StateProps {
  organization?: Organization;
  shouldOrganizationBeDisplayed: boolean;
}

interface OwnProps {
  branches: Branch[];
  component: Component;
  currentBranch?: Branch;
  location?: any;
}

interface Props extends StateProps, OwnProps {}

export function ComponentNavHeader(props: Props) {
  const { component, organization, shouldOrganizationBeDisplayed } = props;

  return (
    <header className="navbar-context-header">
      <OrganizationHelmet
        title={component.name}
        organization={organization && shouldOrganizationBeDisplayed ? organization : undefined}
      />
      {organization &&
        shouldOrganizationBeDisplayed && (
          <>
            <OrganizationAvatar organization={organization} />
            <OrganizationLink
              organization={organization}
              className="link-base-color link-no-underline spacer-left">
              {organization.name}
            </OrganizationLink>
            <span className="slash-separator" />
          </>
        )}
      {renderBreadcrumbs(component.breadcrumbs)}
      {component.visibility === 'private' && (
        <PrivateBadge className="spacer-left" qualifier={component.qualifier} />
      )}
      {props.currentBranch && (
        <ComponentNavBranch
          branches={props.branches}
          component={component}
          currentBranch={props.currentBranch}
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
          className="link-base-color link-no-underline"
          title={item.name}
          to={getProjectUrl(item.key)}>
          {itemName}
        </Link>
        {index < breadcrumbs.length - 1 && <span className="slash-separator" />}
      </React.Fragment>
    );
  });
}

const mapStateToProps = (state: any, ownProps: OwnProps): StateProps => ({
  organization:
    ownProps.component.organization && getOrganizationByKey(state, ownProps.component.organization),
  shouldOrganizationBeDisplayed: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(ComponentNavHeader);
