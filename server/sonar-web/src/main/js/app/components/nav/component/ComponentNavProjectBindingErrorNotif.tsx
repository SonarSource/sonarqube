/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import styled from '@emotion/styled';
import { FlagWarningIcon, Link, themeBorder, themeColor } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { PULL_REQUEST_DECORATION_BINDING_CATEGORY } from '../../../../apps/settings/constants';
import { translate } from '../../../../helpers/l10n';
import { getProjectSettingsUrl } from '../../../../helpers/urls';
import { Component } from '../../../../types/types';

export interface ComponentNavProjectBindingErrorNotifProps {
  component: Component;
}

export default function ComponentNavProjectBindingErrorNotif(
  props: Readonly<ComponentNavProjectBindingErrorNotifProps>,
) {
  const { component } = props;
  let action;

  if (component.configuration?.showSettings) {
    action = (
      <Link to={getProjectSettingsUrl(component.key, PULL_REQUEST_DECORATION_BINDING_CATEGORY)}>
        {translate('component_navigation.pr_deco.action.check_project_settings')}
      </Link>
    );
  } else {
    action = translate('component_navigation.pr_deco.action.contact_project_admin');
  }

  return (
    <StyledBanner className="sw-body-sm sw-py-3 sw-px-4 sw-gap-4">
      <FlagWarningIcon />
      <FormattedMessage id="component_navigation.pr_deco.error_detected_X" values={{ action }} />
    </StyledBanner>
  );
}

const StyledBanner = styled.div`
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: 100%;

  background-color: ${themeColor('warningBackground')};
  border-top: ${themeBorder('default', 'warningBorder')};
  border-bottom: ${themeBorder('default', 'warningBorder')};
`;
