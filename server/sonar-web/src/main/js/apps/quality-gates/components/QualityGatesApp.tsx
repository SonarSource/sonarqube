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
import Helmet from 'react-helmet';
import ListHeader from './ListHeader';
import List from './List';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { fetchQualityGates } from '../../../api/quality-gates';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { Organization, QualityGate } from '../../../app/types';
import '../styles.css';

interface Props {
  children: React.ReactElement<{
    organization?: string;
    refreshQualityGates: () => Promise<void>;
  }>;
  organization: Pick<Organization, 'key'>;
}

interface State {
  canCreate: boolean;
  loading: boolean;
  qualityGates: QualityGate[];
}

export default class QualityGatesApp extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  state: State = { canCreate: false, loading: true, qualityGates: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchQualityGates();

    document.body.classList.add('white-page');
    document.documentElement.classList.add('white-page');
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.add('page-footer-with-sidebar');
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    document.documentElement.classList.remove('white-page');
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.remove('page-footer-with-sidebar');
    }
  }

  fetchQualityGates = () => {
    const { organization } = this.props;
    return fetchQualityGates({ organization: organization && organization.key }).then(
      ({ actions, qualitygates: qualityGates }) => {
        if (this.mounted) {
          this.setState({ canCreate: actions.create, loading: false, qualityGates });

          if (qualityGates && qualityGates.length === 1 && !actions.create) {
            this.context.router.replace(
              getQualityGateUrl(String(qualityGates[0].id), organization && organization.key)
            );
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

  handleSetDefault = (qualityGate: QualityGate) => {
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
    const { children } = this.props;
    const { canCreate, loading, qualityGates } = this.state;
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
                    {qualityGates.length > 0 && (
                      <List organization={organization} qualityGates={qualityGates} />
                    )}
                  </div>
                </div>
              </div>
            )}
          </ScreenPositionHelper>
          {!loading &&
            React.cloneElement(children, {
              onSetDefault: this.handleSetDefault,
              organization,
              qualityGates,
              refreshQualityGates: this.fetchQualityGates
            })}
        </div>
      </>
    );
  }
}
