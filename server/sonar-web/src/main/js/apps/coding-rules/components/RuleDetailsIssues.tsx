/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import { Link } from 'react-router';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Tooltip from '../../../components/controls/Tooltip';
import { getFacet } from '../../../api/issues';
import { getIssuesUrl } from '../../../helpers/urls';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

interface Props {
  organization: string | undefined;
  ruleKey: string;
}

interface Project {
  count: number;
  id: string;
  key: string;
  name: string;
}

interface State {
  loading: boolean;
  projects?: Project[];
  total?: number;
}

export default class RuleDetailsIssues extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    branchesEnabled: PropTypes.bool
  };

  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchIssues();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.ruleKey !== this.props.ruleKey) {
      this.fetchIssues();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIssues = () => {
    this.setState({ loading: true });
    getFacet(
      { organization: this.props.organization, rules: this.props.ruleKey, resolved: false },
      'projectUuids'
    ).then(
      ({ facet, response }) => {
        if (this.mounted) {
          const { components = [], paging } = response;
          const projects = [];
          for (const item of facet) {
            const project = components.find(component => component.uuid === item.val);
            if (project) {
              projects.push({
                count: item.count,
                id: item.val,
                key: project.key,
                name: project.name
              });
            }
          }
          this.setState({ projects, loading: false, total: paging.total });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  renderTotal = () => {
    const { total } = this.state;
    if (total === undefined) {
      return null;
    }
    const path = getIssuesUrl(
      { resolved: 'false', rules: this.props.ruleKey },
      this.props.organization
    );

    const totalItem = (
      <span className="little-spacer-left">
        {'('}
        <Link to={path}>{total}</Link>
        {')'}
      </span>
    );

    if (!this.context.branchesEnabled) {
      return totalItem;
    }

    return (
      <Tooltip overlay={translate('coding_rules.issues.only_main_branches')} placement="right">
        {totalItem}
      </Tooltip>
    );
  };

  renderProject = (project: Project) => {
    const path = getIssuesUrl(
      { projectUuids: project.id, resolved: 'false', rules: this.props.ruleKey },
      this.props.organization
    );
    return (
      <tr key={project.key}>
        <td className="coding-rules-detail-list-name">{project.name}</td>
        <td className="coding-rules-detail-list-parameters">
          <Link to={path}>{formatMeasure(project.count, 'INT')}</Link>
        </td>
      </tr>
    );
  };

  render() {
    const { loading, projects = [] } = this.state;

    return (
      <div className="js-rule-issues coding-rule-section">
        <div className="coding-rule-section-separator" />

        <DeferredSpinner loading={loading}>
          <h3 className="coding-rules-detail-title">
            {translate('coding_rules.issues')}
            {this.renderTotal()}
          </h3>

          {projects.length > 0 && (
            <table className="coding-rules-detail-list coding-rules-most-violated-projects">
              <tbody>
                <tr>
                  <td className="coding-rules-detail-list-name" colSpan={2}>
                    {translate('coding_rules.most_violating_projects')}
                  </td>
                </tr>
                {projects.map(this.renderProject)}
              </tbody>
            </table>
          )}
        </DeferredSpinner>
      </div>
    );
  }
}
