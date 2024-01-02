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
import { Helmet } from 'react-helmet-async';
import { NavigateFunction, useNavigate, useParams } from 'react-router-dom';
import { fetchQualityGates } from '../../../api/quality-gates';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import '../../../components/search-navigator.css';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../../../helpers/pages';
import { getQualityGateUrl } from '../../../helpers/urls';
import { QualityGate } from '../../../types/types';
import '../styles.css';
import Details from './Details';
import List from './List';
import ListHeader from './ListHeader';

interface Props {
  id?: string;
  navigate: NavigateFunction;
}

interface State {
  canCreate: boolean;
  loading: boolean;
  qualityGates: QualityGate[];
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
    if (prevProps.id !== undefined && this.props.id === undefined) {
      this.openDefault(this.state.qualityGates);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
    removeSideBarClass();
  }

  fetchQualityGates = () => {
    return fetchQualityGates().then(
      ({ actions, qualitygates: qualityGates }) => {
        if (this.mounted) {
          this.setState({ canCreate: actions.create, loading: false, qualityGates });

          if (!this.props.id) {
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

  openDefault(qualityGates: QualityGate[]) {
    const defaultQualityGate = qualityGates.find((gate) => Boolean(gate.isDefault))!;
    this.props.navigate(getQualityGateUrl(String(defaultQualityGate.id)), { replace: true });
  }

  handleSetDefault = (qualityGate: QualityGate) => {
    this.setState(({ qualityGates }) => {
      return {
        qualityGates: qualityGates.map((candidate) => {
          if (candidate.isDefault || candidate.id === qualityGate.id) {
            return { ...candidate, isDefault: candidate.id === qualityGate.id };
          }
          return candidate;
        }),
      };
    });
  };

  render() {
    const { id } = this.props;
    const { canCreate, qualityGates } = this.state;
    const defaultTitle = translate('quality_gates.page');

    return (
      <>
        <Helmet defaultTitle={defaultTitle} defer={false} titleTemplate={`%s - ${defaultTitle}`} />
        <div className="layout-page" id="quality-gates-page">
          <Suggestions suggestions="quality_gates" />

          <ScreenPositionHelper className="layout-page-side-outer">
            {({ top }) => (
              <div className="layout-page-side" style={{ top }}>
                <div className="layout-page-side-inner">
                  <div className="layout-page-filters">
                    <ListHeader
                      canCreate={canCreate}
                      refreshQualityGates={this.fetchQualityGates}
                    />
                    <DeferredSpinner loading={this.state.loading}>
                      <List qualityGates={qualityGates} />
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
              qualityGates={this.state.qualityGates}
              refreshQualityGates={this.fetchQualityGates}
            />
          )}
        </div>
      </>
    );
  }
}

export default function AppWrapper() {
  const params = useParams();
  const navigate = useNavigate();

  return <App id={params['id']} navigate={navigate} />;
}
