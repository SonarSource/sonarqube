/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { Link } from 'react-router';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import ChevronsIcon from 'sonar-ui-common/components/icons/ChevronsIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { ComponentQualifier } from '../../../../types/component';

export interface GlobalNavPlusMenuProps {
  canCreateApplication: boolean;
  canCreatePortfolio: boolean;
  canCreateProject: boolean;
  compatibleAlms: Array<string>;
  onComponentCreationClick: (componentQualifier: ComponentQualifier) => void;
}

function renderCreateProjectOptions(compatibleAlms: Array<string>) {
  return [...compatibleAlms, 'manual'].map(alm => (
    <li key={alm}>
      <Link
        className="display-flex-center"
        to={{ pathname: '/projects/create', query: { mode: alm } }}>
        {alm === 'manual' ? (
          <ChevronsIcon className="spacer-right" />
        ) : (
          <img
            alt={alm}
            className="spacer-right"
            width={16}
            src={`${getBaseUrl()}/images/alm/${alm}.svg`}
          />
        )}
        {translate('my_account.add_project', alm)}
      </Link>
    </li>
  ));
}

function renderCreateComponent(
  componentQualifier: ComponentQualifier,
  onClick: (qualifier: ComponentQualifier) => void
) {
  return (
    <li>
      <ButtonLink
        className="display-flex-justify-start padded-left"
        onClick={() => onClick(componentQualifier)}>
        <QualifierIcon className="spacer-right" qualifier={componentQualifier} />
        {translate('my_account.create_new', componentQualifier)}
      </ButtonLink>
    </li>
  );
}

export default function GlobalNavPlusMenu(props: GlobalNavPlusMenuProps) {
  const { canCreateApplication, canCreatePortfolio, canCreateProject, compatibleAlms } = props;

  return (
    <ul className="menu">
      {canCreateProject && (
        <>
          <li className="menu-header">
            <strong>{translate('my_account.add_project')}</strong>
          </li>
          {renderCreateProjectOptions(compatibleAlms)}
        </>
      )}
      {(canCreateApplication || canCreatePortfolio) && (
        <>
          {canCreateProject && <li className="divider" />}
          {canCreatePortfolio &&
            renderCreateComponent(ComponentQualifier.Portfolio, props.onComponentCreationClick)}
          {canCreateApplication &&
            renderCreateComponent(ComponentQualifier.Application, props.onComponentCreationClick)}
        </>
      )}
    </ul>
  );
}
