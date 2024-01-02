/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { getFacet } from '../../../api/issues';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import Link from '../../../components/common/Link';
import Tooltip from '../../../components/controls/Tooltip';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { getIssuesUrl } from '../../../helpers/urls';
import { Feature } from '../../../types/features';
import { RuleDetails } from '../../../types/types';

interface Props extends WithAvailableFeaturesProps {
  ruleDetails: Pick<RuleDetails, 'key' | 'type'>;
}

interface Project {
  count: number;
  key: string;
  name: string;
}

interface State {
  loading: boolean;
  projects?: Project[];
  total?: number;
}

export class RuleDetailsIssues extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchIssues();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.ruleDetails !== this.props.ruleDetails) {
      this.fetchIssues();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIssues = () => {
    const {
      ruleDetails: { key },
    } = this.props;

    this.setState({ loading: true });
    getFacet(
      {
        resolved: 'false',
        rules: key,
      },
      'projects'
    ).then(
      ({ facet, response }) => {
        if (this.mounted) {
          const { components = [], paging } = response;
          const projects = [];
          for (const item of facet) {
            const project = components.find((component) => component.key === item.val);
            if (project) {
              projects.push({ count: item.count, key: project.key, name: project.name });
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
    const {
      ruleDetails: { key },
    } = this.props;

    const { total } = this.state;
    if (total === undefined) {
      return null;
    }
    const path = getIssuesUrl({ resolved: 'false', rules: key });

    const totalItem = (
      <span className="little-spacer-left">
        {'('}
        <Link to={path}>{total}</Link>
        {')'}
      </span>
    );

    if (!this.props.hasFeature(Feature.BranchSupport)) {
      return totalItem;
    }

    return (
      <Tooltip overlay={translate('coding_rules.issues.only_main_branches')}>{totalItem}</Tooltip>
    );
  };

  renderProject = (project: Project) => {
    const {
      ruleDetails: { key },
    } = this.props;

    const path = getIssuesUrl({ resolved: 'false', rules: key, projects: project.key });
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
        <DeferredSpinner loading={loading}>
          <h2 className="coding-rules-detail-title">
            {translate('coding_rules.issues')}
            {this.renderTotal()}
          </h2>

          {projects.length > 0 ? (
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
          ) : (
            <div className="big-padded-bottom">
              {translate('coding_rules.no_issue_detected_for_projects')}
            </div>
          )}
        </DeferredSpinner>
      </div>
    );
  }
}

export default withAvailableFeatures(RuleDetailsIssues);
