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
import moment from 'moment';
import React, { Component } from 'react';

import SeverityHelper from '../../../components/shared/severity-helper';
import { BarChart } from '../../../components/charts/bar-chart';
import { getFacets, getFacet } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';


const BASE_QUERY = { resolved: false, assignees: '__me__' };


function getTotalUrl () {
  return window.baseUrl + '/account/issues#resolved=false';
}

function getToFixUrl () {
  return window.baseUrl + '/account/issues#resolved=false|statuses=CONFIRMED';
}

function getToReviewUrl () {
  return window.baseUrl + '/account/issues#resolved=false|statuses=' + encodeURIComponent('OPEN,REOPENED');
}

function getSeverityUrl (severity) {
  return window.baseUrl + '/account/issues#resolved=false|severities=' + severity;
}

function getProjectUrl (project) {
  return window.baseUrl + '/account/issues#resolved=false|projectUuids=' + project;
}

function getPeriodUrl (createdAfter, createdBefore) {
  return window.baseUrl + `/account/issues#resolved=false|createdAfter=${createdAfter}|createdBefore=${createdBefore}`;
}


export default class IssueWidgets extends Component {
  state = {
    loading: true
  };

  componentDidMount () {
    this.fetchIssues();
  }

  fetchIssues () {
    Promise.all([
      this.fetchFacets(),
      this.fetchByDate()
    ]).then(responses => {
      const facets = responses[0];
      const byDate = responses[1];

      this.setState({
        loading: false,
        total: facets.total,
        severities: facets.severities,
        projects: facets.projects,
        toFix: facets.toFix,
        toReview: facets.toReview,
        byDate
      });
    });
  }

  fetchFacets () {
    return getFacets(BASE_QUERY, ['statuses', 'severities', 'projectUuids']).then(r => {
      const severities = _.sortBy(
          _.findWhere(r.facets, { property: 'severities' }).values,
          (facet) => window.severityComparator(facet.val)
      );

      const projects = _.findWhere(r.facets, { property: 'projectUuids' }).values.map(p => {
        const base = _.findWhere(r.response.components, { uuid: p.val });
        return Object.assign({}, p, base);
      });

      const statuses = _.findWhere(r.facets, { property: 'statuses' }).values;
      const toFix = _.findWhere(statuses, { val: 'CONFIRMED' }).count;
      const toReview = _.findWhere(statuses, { val: 'OPEN' }).count +
          _.findWhere(statuses, { val: 'REOPENED' }).count;

      const total = r.response.total;

      return { severities, projects, toFix, toReview, total };
    });
  }

  fetchByDate () {
    return getFacet(Object.assign({ createdInLast: '1w' }, BASE_QUERY), 'createdAt').then(r => r.facet);
  }

  handleByDateClick ({ value }) {
    const created = moment(value);
    const createdAfter = created.format('YYYY-MM-DD');
    const createdBefore = created.add(1, 'days').format('YYYY-MM-DD');
    window.location = getPeriodUrl(createdAfter, createdBefore);
  }

  renderByDate () {
    const data = this.state.byDate.map((d, x) => {
      return { x, y: d.count, value: d.val };
    });
    const xTicks = this.state.byDate.map(d => moment(d.val).format('dd'));
    const xValues = this.state.byDate.map(d => d.count);

    return (
        <section className="abs-width-300 huge-spacer-top account-bar-chart">
          <h4 className="spacer-bottom">
            {translate('my_account.issue_widget.leak_over_last_week')}
          </h4>
          <BarChart
              data={data}
              xTicks={xTicks}
              xValues={xValues}
              barsWidth={20}
              padding={[25, 0, 25, 0]}
              height={80}
              onBarClick={this.handleByDateClick.bind(this)}/>
        </section>
    );
  }

  render () {
    return (
        <section>
          <h2 className="spacer-bottom">{translate('my_account.my_issues')}</h2>

          {this.state.loading && (
              <i className="spinner"/>
          )}

          {!this.state.loading && (
              <section className="abs-width-300">
                <table className="data zebra">
                  <tbody>
                    <tr>
                      <td>
                        <strong>{translate('total')}</strong>
                      </td>
                      <td className="thin nowrap text-right">
                        <a href={getTotalUrl()}>
                          {formatMeasure(this.state.total, 'SHORT_INT')}
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <span className="spacer-left">{translate('my_account.to_review')}</span>
                      </td>
                      <td className="thin nowrap text-right">
                        <a href={getToReviewUrl()}>
                          {formatMeasure(this.state.toReview, 'SHORT_INT')}
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <span className="spacer-left">{translate('my_account.to_fix')}</span>
                      </td>
                      <td className="thin nowrap text-right">
                        <a href={getToFixUrl()}>
                          {formatMeasure(this.state.toFix, 'SHORT_INT')}
                        </a>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </section>
          )}

          {!this.state.loading && this.renderByDate()}

          {!this.state.loading && (
              <section className="abs-width-300 huge-spacer-top">
                <h4 className="spacer-bottom">
                  {translate('my_account.issue_widget.by_severity')}
                </h4>
                <table className="data zebra">
                  <tbody>
                    {this.state.severities.map(s => (
                        <tr key={s.val}>
                          <td>
                            <SeverityHelper severity={s.val}/>
                          </td>
                          <td className="thin nowrap text-right">
                            <a href={getSeverityUrl(s.val)}>
                              {formatMeasure(s.count, 'SHORT_INT')}
                            </a>
                          </td>
                        </tr>
                    ))}
                  </tbody>
                </table>
              </section>
          )}

          {!this.state.loading && (
              <section className="abs-width-300 huge-spacer-top">
                <h4 className="spacer-bottom">
                  {translate('my_account.issue_widget.by_project')}
                </h4>
                <table className="data zebra">
                  <tbody>
                    {this.state.projects.map(p => (
                        <tr key={p.val}>
                          <td>
                            {p.name}
                          </td>
                          <td className="thin nowrap text-right">
                            <a href={getProjectUrl(p.val)}>
                              {formatMeasure(p.count, 'SHORT_INT')}
                            </a>
                          </td>
                        </tr>
                    ))}
                  </tbody>
                </table>
              </section>
          )}
        </section>
    );
  }
}
