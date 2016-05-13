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
import { render } from 'react-dom';
import { translate } from '../../helpers/l10n';
import { ComplexityDistribution } from '../../components/shared/complexity-distribution';

const Widget = ({ value, of }) => {
  return (
      <div>
        <span className="widget-label">
          {translate(`metric.${of}_complexity_distribution.name`)}
        </span>
        <ComplexityDistribution distribution={value} of={of}/>
      </div>
  );
};

function start ({ el, ...other }) {
  window.sonarqube.appStarted.then(() => {
    const element = document.querySelector(el);
    render(<Widget {...other}/>, element);
  });
}

export default function (options) {
  if (options.value) {
    document.addEventListener('DOMContentLoaded', () => start(options), false);
  }
}
