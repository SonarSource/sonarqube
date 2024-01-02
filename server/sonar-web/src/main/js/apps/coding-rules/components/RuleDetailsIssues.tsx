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
import { ContentCell, Link, Spinner, SubHeadingHighlight, Table, TableRow } from 'design-system';
import { keyBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getComponentData } from '../../../api/components';
import { getFacet } from '../../../api/issues';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import Tooltip from '../../../components/controls/Tooltip';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { getIssuesUrl } from '../../../helpers/urls';
import { Feature } from '../../../types/features';
import { FacetName } from '../../../types/issues';
import { MetricType } from '../../../types/metrics';
import { Dict, RuleDetails } from '../../../types/types';

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
  totalIssues?: number;
  totalProjects?: number;
}

const MAX_VIOLATING_PROJECTS = 10;

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
        ...DEFAULT_ISSUES_QUERY,
        rules: key,
      },
      FacetName.Projects,
    ).then(
      async ({ facet, response }) => {
        if (this.mounted) {
          const { paging } = response;

          this.setState({
            projects: await this.getProjects(facet.slice(0, MAX_VIOLATING_PROJECTS)),
            loading: false,
            totalIssues: paging.total,
            totalProjects: facet.length,
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  /**
   * Retrieve the names of the projects, to display nicely
   * (The facet only contains key & count)
   */
  getProjects = async (facet: { count: number; val: string }[]) => {
    const projects: Dict<{ key: string; name: string }> = keyBy(
      await Promise.all(
        facet.map((item) =>
          getComponentData({ component: item.val })
            .then((response) => ({
              key: item.val,
              name: response.component.name,
            }))
            .catch(() => ({ key: item.val, name: item.val })),
        ),
      ),
      'key',
    );

    return facet.map((item) => {
      return { count: item.count, key: item.val, name: projects[item.val].name };
    });
  };

  renderTotal = () => {
    const {
      ruleDetails: { key },
    } = this.props;

    const { totalIssues: total } = this.state;
    if (total === undefined) {
      return null;
    }
    const path = getIssuesUrl({ ...DEFAULT_ISSUES_QUERY, rules: key });

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

    const path = getIssuesUrl({ ...DEFAULT_ISSUES_QUERY, rules: key, projects: project.key });
    return (
      <TableRow key={project.key}>
        <ContentCell>{project.name}</ContentCell>
        <ContentCell>
          <Link to={path}>{formatMeasure(project.count, MetricType.Integer)}</Link>
        </ContentCell>
      </TableRow>
    );
  };

  render() {
    const { ruleDetails } = this.props;
    const { loading, projects = [], totalProjects } = this.state;

    return (
      <div className="sw-mb-8">
        <Spinner loading={loading}>
          <SubHeadingHighlight as="h2">
            {translate('coding_rules.issues')}
            {this.renderTotal()}
          </SubHeadingHighlight>

          {projects.length > 0 ? (
            <>
              <Table
                className="sw-mt-6"
                columnCount={2}
                header={
                  <TableRow>
                    <ContentCell colSpan={2}>
                      {translate('coding_rules.most_violating_projects')}
                    </ContentCell>
                  </TableRow>
                }
              >
                {projects.map(this.renderProject)}
              </Table>
              {totalProjects !== undefined && totalProjects > projects.length && (
                <div className="sw-text-center sw-mt-4">
                  <FormattedMessage
                    id="coding_rules.most_violating_projects.more_x"
                    values={{
                      count: totalProjects - projects.length,
                      link: (
                        <Link to={getIssuesUrl({ resolved: 'false', rules: ruleDetails.key })}>
                          <FormattedMessage id="coding_rules.most_violating_projects.link" />
                        </Link>
                      ),
                    }}
                  />
                </div>
              )}
            </>
          ) : (
            <div className="sw-mb-6">
              {translate('coding_rules.no_issue_detected_for_projects')}
            </div>
          )}
        </Spinner>
      </div>
    );
  }
}

export default withAvailableFeatures(RuleDetailsIssues);
