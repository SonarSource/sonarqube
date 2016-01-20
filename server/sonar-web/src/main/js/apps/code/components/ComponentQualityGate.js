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

import { translate } from '../../../helpers/l10n';

const METRIC = 'alert_status';

const ComponentQualityGate = ({ component }) => {
  const measure = _.findWhere(component.measures, { metric: METRIC });
  return measure ? (
      <span
          className="spacer-right"
          title={translate('metric.level', measure.value)}
          style={{ position: 'relative', top: '-1px' }}>
        <i className={`icon-alert-${measure.value.toLowerCase()}`}/>
      </span>
  ) : <span/>;
};


export default ComponentQualityGate;
