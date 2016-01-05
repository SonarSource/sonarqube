/*
 * SonarQube :: Web
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

import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';


const Components = ({ baseComponent, components, coverageMetric, onBrowse }) => (
    <table className="data zebra">
      <thead>
        <tr>
          <th className="thin nowrap">&nbsp;</th>
          <th>&nbsp;</th>
          <th className="thin nowrap text-right">{window.t('metric.ncloc.name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.sqale_index.short_name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.violations.name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.coverage.name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.duplicated_lines_density.short_name')}</th>
        </tr>
      </thead>
      {baseComponent && (
          <tbody>
            <Component
                key={baseComponent.key}
                component={baseComponent}
                coverageMetric={coverageMetric}/>
            <tr className="blank">
              <td colSpan="7">&nbsp;</td>
            </tr>
          </tbody>
      )}
      <tbody>
        {components.length ? (
            components.map(component => (
                <Component
                    key={component.key}
                    component={component}
                    coverageMetric={coverageMetric}
                    onBrowse={onBrowse}/>
            ))
        ) : (
            <ComponentsEmpty/>
        )}
      </tbody>
    </table>
);


export default Components;
