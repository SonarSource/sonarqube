/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import { debounce } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getNewCodePeriod, resetNewCodePeriod, setNewCodePeriod } from '../../../api/newCodePeriod';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { BranchLike } from '../../../types/branch-like';
import '../styles.css';
import { getSettingValue } from '../utils';
import BranchList from './BranchList';
import ProjectBaselineSelector from './ProjectBaselineSelector';

interface Props {
  branchLikes: BranchLike[];
  branchesEnabled?: boolean;
  canAdmin?: boolean;
  component: T.Component;
}

interface State {
  analysis?: string;
  currentSetting?: T.NewCodePeriodSettingType;
  currentSettingValue?: string;
  days: string;
  generalSetting?: T.NewCodePeriod;
  loading: boolean;
  overrideGeneralSetting?: boolean;
  saving: boolean;
  selected?: T.NewCodePeriodSettingType;
  success?: boolean;
}

const DEFAULT_GENERAL_SETTING: { type: T.NewCodePeriodSettingType } = {
  type: 'PREVIOUS_VERSION'
};

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    days: '30',
    loading: true,
    saving: false
  };

  // We use debounce as we could have multiple save in less that 3sec.
  resetSuccess = debounce(() => this.setState({ success: undefined }), 3000);

  componentDidMount() {
    this.mounted = true;
    this.fetchLeakPeriodSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getUpdatedState(params: {
    currentSetting?: T.NewCodePeriodSettingType;
    currentSettingValue?: string;
    generalSetting: T.NewCodePeriod;
  }) {
    const { currentSetting, currentSettingValue, generalSetting } = params;

    const defaultDays =
      (!currentSetting && generalSetting.type === 'NUMBER_OF_DAYS' && generalSetting.value) || '30';

    return {
      loading: false,
      currentSetting,
      currentSettingValue,
      generalSetting,
      selected: currentSetting || generalSetting.type,
      overrideGeneralSetting: Boolean(currentSetting),
      days: (currentSetting === 'NUMBER_OF_DAYS' && currentSettingValue) || defaultDays,
      analysis: (currentSetting === 'SPECIFIC_ANALYSIS' && currentSettingValue) || ''
    };
  }

  fetchLeakPeriodSetting() {
    this.setState({ loading: true });

    Promise.all([
      getNewCodePeriod(),
      getNewCodePeriod({
        branch: !this.props.branchesEnabled ? 'master' : undefined,
        project: this.props.component.key
      })
    ]).then(
      ([generalSetting, setting]) => {
        if (this.mounted) {
          if (!generalSetting.type) {
            generalSetting = DEFAULT_GENERAL_SETTING;
          }
          const currentSettingValue = setting.value;
          const currentSetting = setting.inherited ? undefined : setting.type || 'PREVIOUS_VERSION';

          this.setState(
            this.getUpdatedState({ generalSetting, currentSetting, currentSettingValue })
          );
        }
      },
      () => {
        this.setState({ loading: false });
      }
    );
  }

  resetSetting = () => {
    this.setState({ saving: true });
    resetNewCodePeriod({ project: this.props.component.key }).then(
      () => {
        this.setState({
          saving: false,
          currentSetting: undefined,
          selected: undefined,
          success: true
        });
        this.resetSuccess();
      },
      () => {
        this.setState({ saving: false });
      }
    );
  };

  handleSelectAnalysis = (analysis: T.ParsedAnalysis) => this.setState({ analysis: analysis.key });

  handleSelectDays = (days: string) => this.setState({ days });

  handleCancel = () =>
    this.setState(
      ({ generalSetting = DEFAULT_GENERAL_SETTING, currentSetting, currentSettingValue }) =>
        this.getUpdatedState({ generalSetting, currentSetting, currentSettingValue })
    );

  handleSelectSetting = (selected?: T.NewCodePeriodSettingType) => this.setState({ selected });

  handleToggleSpecificSetting = (overrideGeneralSetting: boolean) =>
    this.setState({ overrideGeneralSetting });

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { component } = this.props;
    const { analysis, days, selected: type, overrideGeneralSetting } = this.state;

    if (!overrideGeneralSetting) {
      this.resetSetting();
      return;
    }

    const value = getSettingValue({ type, analysis, days });

    if (type) {
      this.setState({ saving: true });
      setNewCodePeriod({
        project: component.key,
        type,
        value
      }).then(
        () => {
          this.setState({
            saving: false,
            currentSetting: type,
            currentSettingValue: value || undefined,
            success: true
          });
          this.resetSuccess();
        },
        () => {
          this.setState({ saving: false });
        }
      );
    }
  };

  renderHeader() {
    return (
      <header className="page-header">
        <h1 className="page-title">{translate('project_baseline.page')}</h1>
        <p className="page-description">
          <FormattedMessage
            defaultMessage={translate('project_baseline.page.description')}
            id="project_baseline.page.description"
            values={{
              link: (
                <Link to="/documentation/project-administration/new-code-period/">
                  {translate('project_baseline.page.description.link')}
                </Link>
              )
            }}
          />
          <br />
          {this.props.canAdmin && (
            <FormattedMessage
              defaultMessage={translate('project_baseline.page.description2')}
              id="project_baseline.page.description2"
              values={{
                link: (
                  <Link to="/admin/settings?category=new_code_period">
                    {translate('project_baseline.page.description2.link')}
                  </Link>
                )
              }}
            />
          )}
        </p>
      </header>
    );
  }

  render() {
    const { branchLikes, branchesEnabled, component } = this.props;
    const {
      analysis,
      currentSetting,
      days,
      generalSetting,
      loading,
      currentSettingValue,
      overrideGeneralSetting,
      saving,
      selected,
      success
    } = this.state;

    return (
      <>
        <Suggestions suggestions="project_baseline" />
        <div className="page page-limited">
          {this.renderHeader()}
          {loading ? (
            <DeferredSpinner />
          ) : (
            <div className="panel-white project-baseline">
              {branchesEnabled && <h2>{translate('project_baseline.default_setting')}</h2>}

              {generalSetting && overrideGeneralSetting !== undefined && (
                <ProjectBaselineSelector
                  analysis={analysis}
                  branchesEnabled={branchesEnabled}
                  component={component.key}
                  currentSetting={currentSetting}
                  currentSettingValue={currentSettingValue}
                  days={days}
                  generalSetting={generalSetting}
                  onCancel={this.handleCancel}
                  onSelectAnalysis={this.handleSelectAnalysis}
                  onSelectDays={this.handleSelectDays}
                  onSelectSetting={this.handleSelectSetting}
                  onSubmit={this.handleSubmit}
                  onToggleSpecificSetting={this.handleToggleSpecificSetting}
                  overrideGeneralSetting={overrideGeneralSetting}
                  saving={saving}
                  selected={selected}
                />
              )}

              <div className={classNames('spacer-top', { invisible: saving || !success })}>
                <span className="text-success">
                  <AlertSuccessIcon className="spacer-right" />
                  {translate('settings.state.saved')}
                </span>
              </div>
              {generalSetting && branchesEnabled && (
                <div className="huge-spacer-top branch-baseline-selector">
                  <hr />
                  <h2>{translate('project_baseline.configure_branches')}</h2>
                  <BranchList
                    branchLikes={branchLikes}
                    component={component}
                    inheritedSetting={
                      currentSetting
                        ? {
                            type: currentSetting,
                            value: currentSettingValue
                          }
                        : generalSetting
                    }
                  />
                </div>
              )}
            </div>
          )}
        </div>
      </>
    );
  }
}
