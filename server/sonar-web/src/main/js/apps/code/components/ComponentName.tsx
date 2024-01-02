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
import * as React from 'react';
import { colors } from '../../../app/theme';
import Link from '../../../components/common/Link';
import BranchIcon from '../../../components/icons/BranchIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { CodeScope, getComponentOverviewUrl, queryToSearch } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import {
  ComponentQualifier,
  isApplication,
  isPortfolioLike,
  isProject,
} from '../../../types/component';
import { ComponentMeasure } from '../../../types/types';
import { mostCommonPrefix } from '../utils';

export function getTooltip(component: ComponentMeasure) {
  const isFile = component.qualifier === 'FIL' || component.qualifier === 'UTS';

  if (isFile && component.path) {
    return component.path + '\n\n' + component.key;
  }

  return [component.name, component.key, component.branch].filter((s) => !!s).join('\n\n');
}

export interface Props {
  branchLike?: BranchLike;
  canBrowse?: boolean;
  component: ComponentMeasure;
  previous?: ComponentMeasure;
  rootComponent: ComponentMeasure;
  unclickable?: boolean;
  newCodeSelected?: boolean;
}

export default function ComponentName({
  branchLike,
  component,
  unclickable = false,
  rootComponent,
  previous,
  canBrowse = false,
  newCodeSelected,
}: Props) {
  const ariaLabel = unclickable ? translate('code.parent_folder') : undefined;

  if (
    [ComponentQualifier.Application, ComponentQualifier.Portfolio].includes(
      rootComponent.qualifier as ComponentQualifier
    ) &&
    [ComponentQualifier.Application, ComponentQualifier.Project].includes(
      component.qualifier as ComponentQualifier
    )
  ) {
    return (
      <span className="max-width-100 display-inline-flex-center">
        <span className="text-ellipsis" title={getTooltip(component)} aria-label={ariaLabel}>
          {renderNameWithIcon(
            branchLike,
            component,
            previous,
            rootComponent,
            unclickable,
            canBrowse,
            newCodeSelected
          )}
        </span>
        {component.branch ? (
          <span className="text-ellipsis spacer-left">
            <BranchIcon className="little-spacer-right" />
            <span className="note">{component.branch}</span>
          </span>
        ) : (
          <span className="spacer-left badge flex-1">{translate('branches.main_branch')}</span>
        )}
      </span>
    );
  }
  return (
    <span
      className="max-width-100 display-inline-block text-ellipsis"
      title={getTooltip(component)}
      aria-label={ariaLabel}
    >
      {renderNameWithIcon(branchLike, component, previous, rootComponent, unclickable, canBrowse)}
    </span>
  );
}

function renderNameWithIcon(
  branchLike: BranchLike | undefined,
  component: ComponentMeasure,
  previous: ComponentMeasure | undefined,
  rootComponent: ComponentMeasure,
  unclickable = false,
  canBrowse = false,
  newCodeSelected = true
) {
  const name = renderName(component, previous);
  const codeType = newCodeSelected ? CodeScope.New : CodeScope.Overall;
  if (
    !unclickable &&
    (isPortfolioLike(component.qualifier) ||
      isApplication(component.qualifier) ||
      isProject(component.qualifier))
  ) {
    const branch = [ComponentQualifier.Application, ComponentQualifier.Portfolio].includes(
      rootComponent.qualifier as ComponentQualifier
    )
      ? component.branch
      : undefined;
    return (
      <Link
        className="display-inline-flex-center link-no-underline"
        to={getComponentOverviewUrl(
          component.refKey || component.key,
          component.qualifier,
          { branch },
          codeType
        )}
      >
        <QualifierIcon
          className="little-spacer-right"
          qualifier={component.qualifier}
          fill={colors.primary}
        />
        <span>{name}</span>
      </Link>
    );
  } else if (canBrowse) {
    const query = { id: rootComponent.key, ...getBranchLikeQuery(branchLike) };
    if (component.key !== rootComponent.key) {
      Object.assign(query, { selected: component.key });
    }
    return (
      <Link
        className="display-inline-flex-center link-no-underline"
        to={{ pathname: '/code', search: queryToSearch(query) }}
      >
        <QualifierIcon
          className="little-spacer-right"
          qualifier={component.qualifier}
          fill={colors.primary}
        />
        <span>{name}</span>
      </Link>
    );
  }
  return (
    <span>
      <QualifierIcon
        qualifier={component.qualifier}
        fill={
          component.qualifier === ComponentQualifier.Directory ? colors.orange : colors.neutral800
        }
      />{' '}
      {name}
    </span>
  );
}

function renderName(component: ComponentMeasure, previous: ComponentMeasure | undefined) {
  const areBothDirs = component.qualifier === 'DIR' && previous && previous.qualifier === 'DIR';
  const prefix =
    areBothDirs && previous !== undefined
      ? mostCommonPrefix([component.name + '/', previous.name + '/'])
      : '';
  return prefix ? (
    <span>
      <span style={{ color: colors.secondFontColor }}>{prefix}</span>
      <span>{component.name.slice(prefix.length)}</span>
    </span>
  ) : (
    component.name
  );
}
