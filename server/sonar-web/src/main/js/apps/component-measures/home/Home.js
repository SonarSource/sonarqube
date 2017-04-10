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
import { Link, IndexLink } from 'react-router';
import LeakPeriodLegend from '../components/LeakPeriodLegend';
import { getLeakPeriod } from '../../../helpers/periods';
import { translate, getLocalizedMetricDomain } from '../../../helpers/l10n';

export default class Home extends React.Component {
  componentDidMount() {
    document.querySelector('html').classList.add('dashboard-page');
    this.props.onDisplay();
    this.props.fetchMeasures();
  }

  componentWillUnmount() {
    document.querySelector('html').classList.remove('dashboard-page');
  }

  render() {
    const { component, domains, periods } = this.props;

    if (domains == null) {
      return null;
    }

    const leakPeriod = getLeakPeriod(periods);

    return (
      <section id="component-measures-home" className="page page-container page-limited">
        <header id="component-measures-home-header" className="home-header">
          <nav className="nav-pills pull-left">
            <ul>
              <li>
                <IndexLink
                  to={{ pathname: '/component_measures', query: { id: component.key } }}
                  activeClassName="active">
                  {translate('all')}
                </IndexLink>
              </li>
              {domains.map(domain => (
                <li key={domain.name}>
                  <Link
                    to={{
                      pathname: `/component_measures/domain/${domain.name}`,
                      query: { id: component.key }
                    }}
                    activeClassName="active">
                    {getLocalizedMetricDomain(domain.name)}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          {leakPeriod != null && <LeakPeriodLegend period={leakPeriod} />}
        </header>

        <main id="component-measures-home-main">
          {this.props.children}
        </main>
      </section>
    );
  }
}
