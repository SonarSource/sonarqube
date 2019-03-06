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
import * as React from 'react';
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import VulnerabilityList from './VulnerabilityList';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { translate } from '../../../helpers/l10n';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Checkbox from '../../../components/controls/Checkbox';
import NotFound from '../../../app/components/NotFound';
import { getSecurityHotspots } from '../../../api/security-reports';
import { isLongLivingBranch } from '../../../helpers/branches';
import DocTooltip from '../../../components/docs/DocTooltip';
import { StandardType } from '../utils';
import { Alert } from '../../../components/ui/Alert';
import { withRouter, Location, Router } from '../../../components/hoc/withRouter';
import '../style.css';

interface Props {
  branchLike?: T.BranchLike;
  component: T.Component;
  location: Pick<Location, 'pathname' | 'query'>;
  params: { type: string };
  router: Pick<Router, 'push'>;
}

interface State {
  loading: boolean;
  findings: T.SecurityHotspot[];
  hasVulnerabilities: boolean;
  type: StandardType;
  showCWE: boolean;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      findings: [],
      hasVulnerabilities: false,
      type: props.params.type === 'owasp_top_10' ? 'owaspTop10' : 'sansTop25',
      showCWE: props.location.query.showCWE === 'true'
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchSecurityHotspots();
  }

  componentWillReceiveProps(newProps: Props) {
    if (newProps.location.pathname !== this.props.location.pathname) {
      const showCWE = newProps.location.query.showCWE === 'true';
      const type = newProps.params.type === 'owasp_top_10' ? 'owaspTop10' : 'sansTop25';
      this.setState({ type, showCWE }, this.fetchSecurityHotspots);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSecurityHotspots = () => {
    const { branchLike, component } = this.props;
    this.setState({ loading: true });
    getSecurityHotspots({
      project: component.key,
      standard: this.state.type,
      includeDistribution: this.state.showCWE,
      branch: isLongLivingBranch(branchLike) ? branchLike.name : undefined
    })
      .then(results => {
        if (this.mounted) {
          const hasVulnerabilities = results.categories.some(
            item =>
              item.vulnerabilities +
                item.openSecurityHotspots +
                item.toReviewSecurityHotspots +
                item.wontFixSecurityHotspots >
              0
          );
          this.setState({ hasVulnerabilities, findings: results.categories, loading: false });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  handleCheck = (checked: boolean) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: { id: this.props.component.key, showCWE: checked }
    });
    this.setState({ showCWE: checked }, this.fetchSecurityHotspots);
  };

  renderAdditionalRulesMessage = () => {
    const { findings } = this.state;
    if (findings.length === 0) {
      return null;
    }

    const total = findings.map(f => f.totalRules).reduce((sum, count) => sum + count);
    const active = findings.map(f => f.activeRules).reduce((sum, count) => sum + count);
    if (active >= total) {
      return null;
    }

    return (
      <Alert className="spacer-top" display="inline" variant="info">
        {translate('security_reports.more_rules')}
      </Alert>
    );
  };

  render() {
    const { branchLike, component, params } = this.props;
    const { loading, findings, showCWE, type } = this.state;
    if (params.type !== 'owasp_top_10' && params.type !== 'sans_top_25') {
      return <NotFound withContainer={false} />;
    }
    return (
      <div className="page page-limited" id="security-reports">
        <Suggestions suggestions="security_reports" />
        <Helmet title={translate('security_reports', type, 'page')} />
        <header className="page-header">
          <A11ySkipTarget anchor="security_main" />
          <h1 className="page-title">{translate('security_reports', type, 'page')}</h1>
          <div className="page-description">
            {translate('security_reports', type, 'description')}
            <Link
              className="spacer-left"
              target="_blank"
              to={{ pathname: '/documentation/user-guide/security-reports/' }}>
              {translate('learn_more')}
            </Link>
            {this.renderAdditionalRulesMessage()}
          </div>
        </header>
        <div className="display-inline-flex-center">
          <Checkbox
            checked={showCWE}
            className="spacer-left spacer-right vertical-middle"
            disabled={!this.state.hasVulnerabilities}
            id={'showCWE'}
            onCheck={this.handleCheck}>
            <label className="little-spacer-left" htmlFor={'showCWE'}>
              {translate('security_reports.cwe.show')}
              <DocTooltip
                className="spacer-left"
                doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/security-reports/cwe.md')}
              />
            </label>
          </Checkbox>
        </div>
        <DeferredSpinner loading={loading}>
          <VulnerabilityList
            branchLike={branchLike}
            component={component}
            findings={findings}
            showCWE={showCWE}
            type={type}
          />
        </DeferredSpinner>
      </div>
    );
  }
}

export default withRouter(App);
