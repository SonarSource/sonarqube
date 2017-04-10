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
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import QualifierIcon from '../../../../components/shared/qualifier-icon';
import { getOrganizationByKey, areThereCustomOrganizations } from '../../../../store/rootReducer';
import OrganizationLink from '../../../../components/ui/OrganizationLink';

class ComponentNavBreadcrumbs extends React.Component {
  static propTypes = {
    breadcrumbs: React.PropTypes.array
  };

  render() {
    const { breadcrumbs, organization, shouldOrganizationBeDisplayed } = this.props;

    if (!breadcrumbs) {
      return null;
    }

    const displayOrganization = organization != null && shouldOrganizationBeDisplayed;

    const lastItem = breadcrumbs[breadcrumbs.length - 1];

    const items = breadcrumbs.map((item, index) => {
      return (
        <span key={item.key}>
          {!displayOrganization &&
            index === 0 &&
            <span className="navbar-context-title-qualifier little-spacer-right">
              <QualifierIcon qualifier={lastItem.qualifier} />
            </span>}
          <Link
            to={{ pathname: '/dashboard', query: { id: item.key } }}
            className="link-base-color">
            {index === breadcrumbs.length - 1
              ? <strong>{item.name}</strong>
              : <span>{item.name}</span>}
          </Link>
          {index < breadcrumbs.length - 1 && <span className="slash-separator" />}
        </span>
      );
    });

    return (
      <h2 className="navbar-context-title">
        {displayOrganization &&
          <span>
            <span className="navbar-context-title-qualifier little-spacer-right">
              <QualifierIcon qualifier={lastItem.qualifier} />
            </span>
            <OrganizationLink organization={organization} className="link-base-color">
              {organization.name}
            </OrganizationLink>
            <span className="slash-separator" />
          </span>}
        {items}
      </h2>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  organization: ownProps.component.organization &&
    getOrganizationByKey(state, ownProps.component.organization),
  shouldOrganizationBeDisplayed: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(ComponentNavBreadcrumbs);

export const Unconnected = ComponentNavBreadcrumbs;
