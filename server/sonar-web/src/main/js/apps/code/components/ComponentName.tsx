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
import * as React from 'react';
import { Link } from 'react-router';
import LongLivingBranchIcon from 'sonar-ui-common/components/icons/LongLivingBranchIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../../app/theme';
import { getBranchLikeQuery } from '../../../helpers/branches';

export function getTooltip(component: T.ComponentMeasure) {
  const isFile = component.qualifier === 'FIL' || component.qualifier === 'UTS';
  if (isFile && component.path) {
    return component.path + '\n\n' + component.key;
  } else {
    return component.name + '\n\n' + component.key;
  }
}

export function mostCommonPrefix(strings: string[]) {
  const sortedStrings = strings.slice(0).sort();
  const firstString = sortedStrings[0];
  const firstStringLength = firstString.length;
  const lastString = sortedStrings[sortedStrings.length - 1];
  let i = 0;
  while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
    i++;
  }
  const prefix = firstString.substr(0, i);
  const prefixTokens = prefix.split(/[\s\\/]/);
  const lastPrefixPart = prefixTokens[prefixTokens.length - 1];
  return prefix.substr(0, prefix.length - lastPrefixPart.length);
}

export interface Props {
  branchLike?: T.BranchLike;
  canBrowse?: boolean;
  component: T.ComponentMeasure;
  previous?: T.ComponentMeasure;
  rootComponent: T.ComponentMeasure;
}

export default function ComponentName({
  branchLike,
  component,
  rootComponent,
  previous,
  canBrowse = false
}: Props) {
  const areBothDirs = component.qualifier === 'DIR' && previous && previous.qualifier === 'DIR';
  const prefix =
    areBothDirs && previous !== undefined
      ? mostCommonPrefix([component.name + '/', previous.name + '/'])
      : '';
  const name = prefix ? (
    <span>
      <span style={{ color: colors.secondFontColor }}>{prefix}</span>
      <span>{component.name.substr(prefix.length)}</span>
    </span>
  ) : (
    component.name
  );

  let inner = null;

  if (component.refKey && component.qualifier !== 'SVW') {
    const branch = rootComponent.qualifier === 'APP' ? { branch: component.branch } : {};
    inner = (
      <Link
        className="link-with-icon"
        to={{ pathname: '/dashboard', query: { id: component.refKey, ...branch } }}>
        <QualifierIcon qualifier={component.qualifier} /> <span>{name}</span>
      </Link>
    );
  } else if (canBrowse) {
    const query = { id: rootComponent.key, ...getBranchLikeQuery(branchLike) };
    if (component.key !== rootComponent.key) {
      Object.assign(query, { selected: component.key });
    }
    inner = (
      <Link className="link-with-icon" to={{ pathname: '/code', query }}>
        <QualifierIcon qualifier={component.qualifier} /> <span>{name}</span>
      </Link>
    );
  } else {
    inner = (
      <span>
        <QualifierIcon qualifier={component.qualifier} /> {name}
      </span>
    );
  }

  if (rootComponent.qualifier === 'APP') {
    return (
      <span className="max-width-100 display-inline-flex-center">
        <span className="text-ellipsis" title={getTooltip(component)}>
          {inner}
        </span>
        {component.branch ? (
          <span className="text-ellipsis spacer-left">
            <LongLivingBranchIcon className="little-spacer-right" />
            <span className="note">{component.branch}</span>
          </span>
        ) : (
          <span className="spacer-left badge flex-1">{translate('branches.main_branch')}</span>
        )}
      </span>
    );
  } else {
    return (
      <span
        className="max-width-100 display-inline-block text-ellipsis"
        title={getTooltip(component)}>
        {inner}
      </span>
    );
  }
}
