/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { keyBy } from 'lodash';
import * as React from 'react';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { getRuleDetails, getRulesApp } from '../../api/rules';
import RuleDetailsDescription from '../../apps/coding-rules/components/RuleDetailsDescription';
import RuleDetailsMeta from '../../apps/coding-rules/components/RuleDetailsMeta';
import '../../apps/coding-rules/styles.css';
import { withAppState } from '../hoc/withAppState';

interface Props {
  appState: Pick<T.AppState, 'organizationsEnabled'>;
  onLoad: (details: { name: string }) => void;
  organizationKey: string | undefined;
  ruleKey: string;
}

interface State {
  loading: boolean;
  referencedRepositories: T.Dict<{ key: string; language: string; name: string }>;
  ruleDetails?: T.RuleDetails;
}

export class WorkspaceRuleDetails extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, referencedRepositories: {} };

  componentDidMount() {
    this.mounted = true;
    this.fetchRuleDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.ruleKey !== this.props.ruleKey ||
      prevProps.organizationKey !== this.props.organizationKey
    ) {
      this.fetchRuleDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRuleDetails = () => {
    this.setState({ loading: true });
    Promise.all([
      getRulesApp({ organization: this.props.organizationKey }),
      getRuleDetails({ key: this.props.ruleKey, organization: this.props.organizationKey })
    ]).then(
      ([{ repositories }, { rule }]) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            referencedRepositories: keyBy(repositories, 'key'),
            ruleDetails: rule
          });
          this.props.onLoad({ name: rule.name });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  noOp = () => {};

  render() {
    const { organizationKey } = this.props;
    const { organizationsEnabled } = this.props.appState;
    const organization = organizationsEnabled ? organizationKey : undefined;

    return (
      <DeferredSpinner loading={this.state.loading}>
        {this.state.ruleDetails && (
          <>
            <RuleDetailsMeta
              canWrite={false}
              hideSimilarRulesFilter={true}
              onFilterChange={this.noOp}
              onTagsChange={this.noOp}
              organization={organization}
              referencedRepositories={this.state.referencedRepositories}
              ruleDetails={this.state.ruleDetails}
            />
            <RuleDetailsDescription
              canWrite={false}
              onChange={this.noOp}
              organization={organization}
              ruleDetails={this.state.ruleDetails}
            />
          </>
        )}
      </DeferredSpinner>
    );
  }
}

export default withAppState(WorkspaceRuleDetails);
