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
import { Link, IndexLink } from 'react-router';

import { getLeakPeriodLabel } from '../../../helpers/periods';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class Home extends React.Component {
  componentDidMount () {
    this.props.onDisplay();
    this.props.fetchMeasures();
  }

  render () {
    const { component, domains, periods } = this.props;

    if (domains == null) {
      return null;
    }

    const leakPeriodLabel = getLeakPeriodLabel(periods);

    return (
        <section id="component-measures-home" className="page page-container page-limited">
          <header id="component-measures-home-header" className="home-header">
            <nav className="nav-pills pull-left">
              <ul>
                <li>
                  <IndexLink
                      to={{ pathname: '/', query: { id: component.key } }}
                      activeClassName="active">
                    {translate('all')}
                  </IndexLink>
                </li>
                {domains.map(domain => (
                    <li key={domain.name}>
                      <Link
                          to={{ pathname: `domain/${domain.name}`, query: { id: component.key } }}
                          activeClassName="active">
                        {domain.name}
                      </Link>
                    </li>
                ))}
              </ul>
            </nav>

            {leakPeriodLabel != null && (
                <div className="measures-domains-leak-header">
                  {translateWithParameters('overview.leak_period_x', leakPeriodLabel)}
                </div>
            )}
          </header>

          <main id="component-measures-home-main">
            {this.props.children}
          </main>
        </section>
    );
  }
}
