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
import { WithRouterProps } from 'react-router';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass
} from 'sonar-ui-common/helpers/pages';
import { fetchQualityGates } from '../../../api/quality-gates';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import '../../../components/search-navigator.css';
import { getQualityGateUrl } from '../../../helpers/urls';
import '../styles.css';
import Details from './Details';
import List from './List';
import ListHeader from './ListHeader';

interface Props extends WithRouterProps {
  organization?: Pick<T.Organization, 'key'>;
}

interface State {
  canCreate: boolean;
  loading: boolean;
  qualityGates: T.QualityGate[];
}

class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { canCreate: false, loading: true, qualityGates: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchQualityGates();
    addWhitePageClass();
    addSideBarClass();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.params.id !== undefined && this.props.params.id === undefined) {
      this.openDefault(this.state.qualityGates);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
    removeSideBarClass();
  }

  fetchQualityGates = () => {
    const { organization } = this.props;
    return fetchQualityGates({ organization: organization && organization.key }).then(
      ({ actions, qualitygates: qualityGates }) => {
        if (this.mounted) {
          this.setState({ canCreate: actions.create, loading: false, qualityGates });

          if (!this.props.params.id) {
            this.openDefault(qualityGates);
          }
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  openDefault(qualityGates: T.QualityGate[]) {
    const defaultQualityGate = qualityGates.find(gate => Boolean(gate.isDefault))!;
    const { organization } = this.props;
    this.props.router.replace(
      getQualityGateUrl(String(defaultQualityGate.id), organization && organization.key)
    );
  }

  handleSetDefault = (qualityGate: T.QualityGate) => {
    this.setState(({ qualityGates }) => {
      return {
        qualityGates: qualityGates.map(candidate => {
          if (candidate.isDefault || candidate.id === qualityGate.id) {
            return { ...candidate, isDefault: candidate.id === qualityGate.id };
          }
          return candidate;
        })
      };
    });
  };

  render() {
    const { id } = this.props.params;
    const { canCreate, qualityGates } = this.state;
    const defaultTitle = translate('quality_gates.page');
    const organization = this.props.organization && this.props.organization.key;

    return (
      <>
        <Helmet defaultTitle={defaultTitle} titleTemplate={'%s - ' + defaultTitle} />
        <div className="layout-page" id="quality-gates-page">
          <Suggestions suggestions="quality_gates" />

          <ScreenPositionHelper className="layout-page-side-outer">
            {({ top }) => (
              <div className="layout-page-side" style={{ top }}>
                <div className="layout-page-side-inner">
                  <div className="layout-page-filters">
                    <ListHeader
                      canCreate={canCreate}
                      organization={organization}
                      refreshQualityGates={this.fetchQualityGates}
                    />
                    <DeferredSpinner loading={this.state.loading}>
                      <List organization={organization} qualityGates={qualityGates} />
                    </DeferredSpinner>
                  </div>
                </div>
              </div>
            )}
          </ScreenPositionHelper>

          {id !== undefined && (
            <Details
              id={id}
              onSetDefault={this.handleSetDefault}
              organization={organization}
              qualityGates={this.state.qualityGates}
              refreshQualityGates={this.fetchQualityGates}
            />
          )}
        </div>
      </>
    );
  }
}

export default App;
