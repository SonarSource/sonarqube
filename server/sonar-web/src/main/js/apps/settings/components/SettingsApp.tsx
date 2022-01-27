/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { find } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { connect } from 'react-redux';
import { WithRouterProps } from 'react-router';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { translate } from '../../../helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass
} from '../../../helpers/pages';
import { getSettingsAppDefaultCategory, Store } from '../../../store/rootReducer';
import { Component } from '../../../types/types';
import { fetchSettings } from '../store/actions';
import '../styles.css';
import { ADDITIONAL_CATEGORIES } from './AdditionalCategories';
import AllCategoriesList from './AllCategoriesList';
import CategoryDefinitionsList from './CategoryDefinitionsList';
import CATEGORY_OVERRIDES from './CategoryOverrides';
import PageHeader from './PageHeader';

interface Props {
  component?: Component;
  defaultCategory: string;
  fetchSettings(component?: string): Promise<void>;
}

interface State {
  loading: boolean;
}

export class SettingsApp extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    addSideBarClass();
    addWhitePageClass();
    this.fetchSettings();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component) {
      this.fetchSettings();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeSideBarClass();
    removeWhitePageClass();
  }

  fetchSettings = () => {
    const { component } = this.props;
    this.props.fetchSettings(component && component.key).then(this.stopLoading, this.stopLoading);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    if (this.state.loading) {
      return null;
    }

    const { query } = this.props.location;
    const originalCategory = (query.category as string) || this.props.defaultCategory;
    const overriddenCategory = CATEGORY_OVERRIDES[originalCategory.toLowerCase()];
    const selectedCategory = overriddenCategory || originalCategory;
    const foundAdditionalCategory = find(ADDITIONAL_CATEGORIES, c => c.key === selectedCategory);
    const isProjectSettings = this.props.component;
    const shouldRenderAdditionalCategory =
      foundAdditionalCategory &&
      ((isProjectSettings && foundAdditionalCategory.availableForProject) ||
        (!isProjectSettings && foundAdditionalCategory.availableGlobally));

    return (
      <div id="settings-page">
        <Suggestions suggestions="settings" />
        <Helmet defer={false} title={translate('settings.page')} />
        <PageHeader component={this.props.component} />

        <div className="layout-page">
          <ScreenPositionHelper className="layout-page-side-outer">
            {({ top }) => (
              <div className="layout-page-side" style={{ top }}>
                <div className="layout-page-side-inner">
                  <AllCategoriesList
                    component={this.props.component}
                    defaultCategory={this.props.defaultCategory}
                    selectedCategory={selectedCategory}
                  />
                </div>
              </div>
            )}
          </ScreenPositionHelper>

          <div className="layout-page-main">
            <div className="layout-page-main-inner">
              <div className="big-padded">
                {foundAdditionalCategory && shouldRenderAdditionalCategory ? (
                  foundAdditionalCategory.renderComponent({
                    component: this.props.component,
                    selectedCategory: originalCategory
                  })
                ) : (
                  <CategoryDefinitionsList
                    category={selectedCategory}
                    component={this.props.component}
                  />
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  defaultCategory: getSettingsAppDefaultCategory(state)
});

const mapDispatchToProps = { fetchSettings: fetchSettings as any };

export default connect(mapStateToProps, mapDispatchToProps)(SettingsApp);
