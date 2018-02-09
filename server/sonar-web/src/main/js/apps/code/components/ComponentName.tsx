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
import { Link } from 'react-router';
import Truncated from './Truncated';
import * as theme from '../../../app/theme';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import { Component } from '../types';

function getTooltip(component: Component) {
  const isFile = component.qualifier === 'FIL' || component.qualifier === 'UTS';
  if (isFile && component.path) {
    return component.path + '\n\n' + component.key;
  } else {
    return component.name + '\n\n' + component.key;
  }
}

function mostCommitPrefix(strings: string[]) {
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

interface Props {
  branch?: string;
  canBrowse?: boolean;
  component: Component;
  previous?: Component;
  rootComponent: Component;
}

export default function ComponentName(props: Props) {
  const { branch, component, rootComponent, previous, canBrowse = false } = props;
  const areBothDirs = component.qualifier === 'DIR' && previous && previous.qualifier === 'DIR';
  const prefix =
    areBothDirs && previous !== undefined
      ? mostCommitPrefix([component.name + '/', previous.name + '/'])
      : '';
  const name = prefix ? (
    <span>
      <span style={{ color: theme.secondFontColor }}>{prefix}</span>
      <span>{component.name.substr(prefix.length)}</span>
    </span>
  ) : (
    component.name
  );

  let inner = null;

  if (component.refKey && component.qualifier !== 'SVW') {
    inner = (
      <Link
        to={{ pathname: '/dashboard', query: { id: component.refKey } }}
        className="link-with-icon">
        <QualifierIcon qualifier={component.qualifier} /> <span>{name}</span>
      </Link>
    );
  } else if (canBrowse) {
    const query = { id: rootComponent.key, branch };
    if (component.key !== rootComponent.key) {
      Object.assign(query, { selected: component.key });
    }
    inner = (
      <Link to={{ pathname: '/code', query }} className="link-with-icon">
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

  return <Truncated title={getTooltip(component)}>{inner}</Truncated>;
}
