/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import {
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LargeCenteredLayout,
  PageContentFontWrapper,
  Spinner,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { NavigateFunction, useNavigate, useParams } from 'react-router-dom';
import { fetchQualityGates } from '../../../api/quality-gates';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import '../../../components/search-navigator.css';
import { translate, translateWithParameters } from '../../../helpers/l10n';
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
  name?: string;
  navigate: NavigateFunction;
}

interface State {
  canCreate: boolean;
  loading: boolean;
  qualityGates: QualityGate[];
}

const MAIN_CONTENT_TOP_PADDING = 48;

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
    if (prevProps.name !== undefined && this.props.name === undefined) {
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

          if (!this.props.name) {
            this.openDefault(qualityGates);
          }
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  openDefault(qualityGates: QualityGate[]) {
    const defaultQualityGate = qualityGates.find((gate) => Boolean(gate.isDefault))!;
    this.props.navigate(getQualityGateUrl(defaultQualityGate.name), { replace: true });
  }

  handleSetDefault = (qualityGate: QualityGate) => {
    this.setState(({ qualityGates }) => {
      return {
        qualityGates: qualityGates.map((candidate) => {
          if (candidate.isDefault || candidate.name === qualityGate.name) {
            return { ...candidate, isDefault: candidate.name === qualityGate.name };
          }
          return candidate;
        }),
      };
    });
  };

  render() {
    const { name } = this.props;
    const { canCreate, qualityGates } = this.state;

    return (
      <LargeCenteredLayout id="quality-gates-page">
        <PageContentFontWrapper className="sw-body-sm">
          <Helmet
            defer={false}
            titleTemplate={translateWithParameters(
              'page_title.template.with_category',
              translate('quality_gates.page'),
            )}
          />
          <div className="sw-grid sw-gap-x-12 sw-gap-y-6 sw-grid-cols-12 sw-w-full">
            <Suggestions suggestions="quality_gates" />

            <StyledContentWrapper
              className="sw-col-span-3 sw-px-4 sw-py-6 sw-border-t-0 sw-rounded-0"
              style={{
                height: `calc(100vh - ${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_FOOTER_HEIGHT}px)`,
              }}
            >
              <ListHeader canCreate={canCreate} refreshQualityGates={this.fetchQualityGates} />
              <Spinner loading={this.state.loading}>
                <List qualityGates={qualityGates} currentQualityGate={name} />
              </Spinner>
            </StyledContentWrapper>

            {name !== undefined && (
              <StyledContentWrapper
                className="sw-col-span-9 sw-overflow-y-auto sw-mt-12"
                style={{
                  height: `calc(100vh - ${
                    LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_FOOTER_HEIGHT
                  }px - ${MAIN_CONTENT_TOP_PADDING}px)`,
                }}
              >
                <Details
                  qualityGateName={name}
                  onSetDefault={this.handleSetDefault}
                  refreshQualityGates={this.fetchQualityGates}
                />
              </StyledContentWrapper>
            )}
          </div>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default function AppWrapper() {
  const params = useParams();
  const navigate = useNavigate();

  return <App name={params['name']} navigate={navigate} />;
}

const StyledContentWrapper = withTheme(styled.div`
  box-sizing: border-box;
  border-radius: 4px;
  background-color: ${themeColor('filterbar')};
  border: ${themeBorder('default', 'filterbarBorder')};
  border-bottom: none;
  overflow-x: hidden;
`);
