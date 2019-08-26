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
import * as classNames from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import VisibleIcon from 'sonar-ui-common/components/icons/VisibleIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../app/theme';
import { isCurrentUserMemberOf, isPaidOrganization } from '../../helpers/organizations';
import { isSonarCloud } from '../../helpers/system';
import {
  getCurrentUser,
  getMyOrganizations,
  getOrganizationByKey,
  Store
} from '../../store/rootReducer';
import DocTooltip from '../docs/DocTooltip';

interface StateToProps {
  currentUser: T.CurrentUser;
  organization?: T.Organization;
  userOrganizations: T.Organization[];
}

interface OwnProps {
  className?: string;
  organization: T.Organization | string | undefined;
  qualifier: string;
  tooltipProps?: { projectKey: string };
  visibility: T.Visibility;
}

interface Props extends OwnProps, StateToProps {
  organization: T.Organization | undefined;
}

export function PrivacyBadge({
  className,
  currentUser,
  organization,
  qualifier,
  userOrganizations,
  tooltipProps,
  visibility
}: Props) {
  const onSonarCloud = isSonarCloud();
  if (
    visibility !== 'private' &&
    (!onSonarCloud || !isCurrentUserMemberOf(currentUser, organization, userOrganizations))
  ) {
    return null;
  }

  let icon = null;
  if (isPaidOrganization(organization) && visibility === 'public') {
    icon = <VisibleIcon className="little-spacer-right" fill={colors.blue} />;
  }

  const badge = (
    <div
      className={classNames('badge', className, {
        'badge-info': Boolean(icon)
      })}>
      {icon}
      {translate('visibility', visibility)}
    </div>
  );

  if (onSonarCloud && organization) {
    return (
      <DocTooltip
        className={className}
        doc={getDoc(visibility, icon, organization)}
        overlayProps={{ ...tooltipProps, organization: organization.key }}>
        {badge}
      </DocTooltip>
    );
  }

  return (
    <Tooltip overlay={translate('visibility', visibility, 'description', qualifier)}>
      {badge}
    </Tooltip>
  );
}

const mapStateToProps = (state: Store, { organization }: OwnProps) => {
  if (typeof organization === 'string') {
    organization = getOrganizationByKey(state, organization);
  }
  return {
    currentUser: getCurrentUser(state),
    organization,
    userOrganizations: getMyOrganizations(state)
  };
};

export default connect(mapStateToProps)(PrivacyBadge);

function getDoc(visibility: T.Visibility, icon: JSX.Element | null, organization: T.Organization) {
  let doc;
  const { actions = {} } = organization;
  if (visibility === 'private') {
    doc = import(/* webpackMode: "eager" */ 'Docs/tooltips/project/visibility-private.md');
  } else if (icon) {
    if (actions.admin) {
      doc = import(/* webpackMode: "eager" */ 'Docs/tooltips/project/visibility-public-paid-org-admin.md');
    } else {
      doc = import(/* webpackMode: "eager" */ 'Docs/tooltips/project/visibility-public-paid-org.md');
    }
  } else if (actions.admin) {
    doc = import(/* webpackMode: "eager" */ 'Docs/tooltips/project/visibility-public-admin.md');
  } else {
    doc = import(/* webpackMode: "eager" */ 'Docs/tooltips/project/visibility-public.md');
  }
  return doc;
}
