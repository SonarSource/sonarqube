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
import classNames from 'classnames';
import QualifierIcon from '../../../../components/shared/qualifier-icon';
import { splitPath } from '../../../../helpers/path';
import { getComponentUrl } from '../../../../helpers/urls';

const ComponentCell = ({ component, isSelected, onClick }) => {
  const linkClassName = classNames('link-no-underline', {
    selected: isSelected
  });

  const handleClick = e => {
    const isLeftClickEvent = e.button === 0;
    const isModifiedEvent = !!(e.metaKey || e.altKey || e.ctrlKey || e.shiftKey);

    if (isLeftClickEvent && !isModifiedEvent) {
      e.preventDefault();
      onClick();
    }
  };

  let head = '';
  let tail = component.name;

  if (['DIR', 'FIL', 'UTS'].includes(component.qualifier)) {
    const parts = splitPath(component.path);
    head = parts.head;
    tail = parts.tail;
  }

  const inner = (
    <span title={component.refKey || component.key}>
      <QualifierIcon qualifier={component.qualifier} />
      &nbsp;
      {head.length > 0 && <span className="note">{head}/</span>}
      <span>{tail}</span>
    </span>
  );

  return (
    <td style={{ maxWidth: 0 }}>
      <div
        style={{
          maxWidth: '100%',
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis'
        }}>
        {component.refId == null || component.qualifier === 'DEV_PRJ'
          ? <a
              id={'component-measures-component-link-' + component.key}
              className={linkClassName}
              href={getComponentUrl(component.key)}
              onClick={handleClick}>
              {inner}
            </a>
          : <a
              id={'component-measures-component-link-' + component.key}
              className={linkClassName}
              href={getComponentUrl(component.refKey || component.key)}>
              <span className="big-spacer-right">
                <i className="icon-detach" />
              </span>

              {inner}
            </a>}
      </div>
    </td>
  );
};

export default ComponentCell;
