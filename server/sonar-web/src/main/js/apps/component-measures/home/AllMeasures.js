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

import Spinner from './../components/Spinner';
import AllMeasuresDomain from './AllMeasuresDomain';
import { getLeakPeriodLabel } from '../../../helpers/periods';

export default class AllMeasures extends React.Component {
  componentDidMount () {
    this.props.onDisplay();
    this.props.fetchMeasures();
  }

  render () {
    const { component, measures, periods, fetching } = this.props;

    if (fetching) {
      return <Spinner/>;
    }

    const domains = _.sortBy(_.pairs(_.groupBy(measures, measure => measure.metric.domain)).map(r => {
      const [name, measures] = r;
      const sortedMeasures = _.sortBy(measures, measure => measure.metric.name);

      return { name, measures: sortedMeasures };
    }), 'name');

    const leakPeriodLabel = getLeakPeriodLabel(periods);

    return (
        <section id="component-measures-home" className="page page-container page-limited">
          <ul className="measures-domains">
            {domains.map((domain, index) => (
                <AllMeasuresDomain
                    key={domain.name}
                    domain={domain}
                    component={component}
                    displayLeakHeader={index === 0}
                    leakPeriodLabel={leakPeriodLabel}/>
            ))}
          </ul>
        </section>
    );
  }
}
