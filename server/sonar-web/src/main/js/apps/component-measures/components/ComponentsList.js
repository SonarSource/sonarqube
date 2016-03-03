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
import React from 'react';

import UpIcon from './UpIcon';
import QualifierIcon from '../../../components/shared/qualifier-icon';
import { formatMeasure } from '../../../helpers/measures';
import { formatLeak } from '../utils';

export default function ComponentsList ({ components, selected, parent, metric, onClick }) {
  const handleClick = (component, e) => {
    e.preventDefault();
    e.target.blur();
    onClick(component);
  };

  return (
      <ul>
        {parent && (
            <li key={parent.id} className="measure-details-components-parent">
              <a href="#" onClick={handleClick.bind(this, parent)}>
                <div className="measure-details-component-name">
                  <UpIcon/>
                  &nbsp;
                  ..
                </div>
              </a>
            </li>
        )}
        {components.map(component => (
            <li key={component.id}>
              <a
                  className={component === selected ? 'selected' : undefined}
                  href="#"
                  onClick={handleClick.bind(this, component)}>

                <div className="measure-details-component-name">
                  <QualifierIcon qualifier={component.qualifier}/>
                  &nbsp;
                  <span>{component.name}</span>
                </div>

                <div className="measure-details-component-value">
                  {component.value != null ? (
                      formatMeasure(component.value, metric.type)
                  ) : (
                      component.leak != null && (
                          formatLeak(component.leak, metric)
                      )
                  )}
                </div>

              </a>
            </li>
        ))}
      </ul>
  );
}
