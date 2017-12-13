/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Component, Organization } from '../../../types';
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
  component: Component;
}

interface Props extends StateProps, OwnProps {}

export function ComponentNavBreadcrumbs(props: Props) {
  const { component, organization, shouldOrganizationBeDisplayed } = props;
  const { breadcrumbs } = component;

  const lastItem = breadcrumbs[breadcrumbs.length - 1];

  const items: JSX.Element[] = [];
  breadcrumbs.forEach((item, index) => {
    const isPath = item.qualifier === 'DIR';
    const itemName = isPath ? collapsePath(item.name, 15) : limitComponentName(item.name);

    if (index === 0) {
      items.push(
        <QualifierIcon
          className="spacer-right"
          key={`qualifier-${item.key}`}
          qualifier={lastItem.qualifier}
        />
      );
    }

    items.push(
      <Link
        className="link-base-color link-no-underline"
        key={`name-${item.key}`}
        title={item.name}
        to={getProjectUrl(item.key)}>
        {itemName}
      </Link>
    );

    if (index < breadcrumbs.length - 1) {
      items.push(<span className="slash-separator" key={`separator-${item.key}`} />);
    }
  });

  return (
    <header className="navbar-context-header">
      <OrganizationHelmet
        title={component.name}
        organization={organization && shouldOrganizationBeDisplayed ? organization : undefined}
      />
      {organization &&
        shouldOrganizationBeDisplayed && <OrganizationAvatar organization={organization} />}
      {organization &&
        shouldOrganizationBeDisplayed && (
          <OrganizationLink
            organization={organization}
            className="link-base-color link-no-underline spacer-left">
            {organization.name}
          </OrganizationLink>
        )}
      {organization && shouldOrganizationBeDisplayed && <span className="slash-separator" />}
      {items}
      {component.visibility === 'private' && (
        <PrivateBadge className="spacer-left" qualifier={component.qualifier} />
      )}
    </header>
  );
}

const mapStateToProps = (state: any, ownProps: OwnProps): StateProps => ({
  organization:
    ownProps.component.organization && getOrganizationByKey(state, ownProps.component.organization),
  shouldOrganizationBeDisplayed: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(ComponentNavBreadcrumbs);
