/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import React from 'react';

import Truncated from './Truncated';
import QualifierIcon from '../../../components/shared/qualifier-icon';


function getTooltip (component) {
  const isFile = component.qualifier === 'FIL' || component.qualifier === 'UTS';
  if (isFile && component.path) {
    return component.path;
  } else {
    return component.name;
  }
}

function mostCommitPrefix (strings) {
  var sortedStrings = strings.slice(0).sort(),
      firstString = sortedStrings[0],
      firstStringLength = firstString.length,
      lastString = sortedStrings[sortedStrings.length - 1],
      i = 0;
  while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
    i++;
  }
  var prefix = firstString.substr(0, i),
      lastPrefixPart = _.last(prefix.split(/[\s\\\/]/));
  return prefix.substr(0, prefix.length - lastPrefixPart.length);
}


const Component = ({ component, previous, onBrowse }) => {
  const handleClick = (e) => {
    e.preventDefault();
    onBrowse(component);
  };

  const areBothDirs = component.qualifier === 'DIR' && previous && previous.qualifier === 'DIR';
  const prefix = areBothDirs ? mostCommitPrefix([component.name + '/', previous.name + '/']) : '';
  const name = prefix ? (
      <span>
        <span style={{ color: '#777' }}>{prefix}</span>
        <span>{component.name.substr(prefix.length)}</span>
      </span>
  ) : component.name;
  const canBrowse = !!onBrowse;

  return (
      <Truncated title={getTooltip(component)}>
        <QualifierIcon qualifier={component.qualifier}/>
        {' '}
        {canBrowse ? (
            <a
                onClick={handleClick}
                href="#">
              {name}
            </a>
        ) : (
            <span>
              {name}
            </span>
        )}
      </Truncated>
  );
};


export default Component;
