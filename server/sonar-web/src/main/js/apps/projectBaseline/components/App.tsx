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
import classNames from 'classnames';
import { debounce } from 'lodash';
import * as React from 'react';
import { getNewCodePeriod, resetNewCodePeriod, setNewCodePeriod } from '../../../api/newCodePeriod';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import AlertSuccessIcon from '../../../components/icons/AlertSuccessIcon';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { isBranch, sortBranches } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { AppState } from '../../../types/appstate';
import { Branch, BranchLike } from '../../../types/branch-like';
import { Feature } from '../../../types/features';
import { ParsedAnalysis } from '../../../types/project-activity';
import { Component, NewCodePeriod, NewCodePeriodSettingType } from '../../../types/types';
import '../styles.css';
import { getSettingValue } from '../utils';
import AppHeader from './AppHeader';
import BranchList from './BranchList';
import ProjectBaselineSelector from './ProjectBaselineSelector';

interface Props extends WithAvailableFeaturesProps {
  branchLike: Branch;
  branchLikes: BranchLike[];
  component: Component;
  appState: AppState;
}

interface State {
  analysis?: string;
  branchList: Branch[];
  currentSetting?: NewCodePeriodSettingType;
  currentSettingValue?: string;
  days: string;
  generalSetting?: NewCodePeriod;
  loading: boolean;
  overrideGeneralSetting?: boolean;
  referenceBranch?: string;
  saving: boolean;
  selected?: NewCodePeriodSettingType;
  success?: boolean;
}

const DEFAULT_NUMBER_OF_DAYS = '30';

const DEFAULT_GENERAL_SETTING: { type: NewCodePeriodSettingType } = {
  type: 'PREVIOUS_VERSION',
};

export class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    branchList: [],
    days: DEFAULT_NUMBER_OF_DAYS,
    loading: true,
    saving: false,
  };

  // We use debounce as we could have multiple save in less that 3sec.
  resetSuccess = debounce(() => this.setState({ success: undefined }), 3000);

  componentDidMount() {
    this.mounted = true;
    this.fetchLeakPeriodSetting();
    this.sortAndFilterBranches(this.props.branchLikes);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.branchLikes !== this.props.branchLikes) {
      this.sortAndFilterBranches(this.props.branchLikes);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getUpdatedState(params: {
    currentSetting?: NewCodePeriodSettingType;
    currentSettingValue?: string;
    generalSetting: NewCodePeriod;
  }) {
    const { currentSetting, currentSettingValue, generalSetting } = params;
    const { referenceBranch } = this.state;

    const defaultDays =
      (generalSetting.type === 'NUMBER_OF_DAYS' && generalSetting.value) || DEFAULT_NUMBER_OF_DAYS;

    return {
      loading: false,
      currentSetting,
      currentSettingValue,
      generalSetting,
      selected: currentSetting || generalSetting.type,
      overrideGeneralSetting: Boolean(currentSetting),
      days: (currentSetting === 'NUMBER_OF_DAYS' && currentSettingValue) || defaultDays,
      analysis: (currentSetting === 'SPECIFIC_ANALYSIS' && currentSettingValue) || '',
      referenceBranch:
        (currentSetting === 'REFERENCE_BRANCH' && currentSettingValue) || referenceBranch,
    };
  }

  sortAndFilterBranches(branchLikes: BranchLike[] = []) {
    const branchList = sortBranches(branchLikes.filter(isBranch));
    this.setState({ branchList, referenceBranch: branchList[0].name });
  }

  fetchLeakPeriodSetting() {
    const { branchLike, component } = this.props;

    this.setState({ loading: true });

    Promise.all([
      getNewCodePeriod(),
      getNewCodePeriod({
        branch: branchLike.name,
        project: component.key,
      }),
    ]).then(
      ([generalSetting, setting]) => {
        if (this.mounted) {
          if (!generalSetting.type) {
            generalSetting = DEFAULT_GENERAL_SETTING;
          }
          const currentSettingValue = setting.value;
          const currentSetting = setting.inherited ? undefined : setting.type || 'PREVIOUS_VERSION';

          this.setState(
            this.getUpdatedState({
              generalSetting,
              currentSetting,
              currentSettingValue,
            })
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
          success: true,
        });
        this.resetSuccess();
      },
      () => {
        this.setState({ saving: false });
      }
    );
  };

  handleSelectAnalysis = (analysis: ParsedAnalysis) => this.setState({ analysis: analysis.key });

  handleSelectDays = (days: string) => this.setState({ days });

  handleSelectReferenceBranch = (referenceBranch: string) => {
    this.setState({ referenceBranch });
  };

  handleCancel = () =>
    this.setState(
      ({ generalSetting = DEFAULT_GENERAL_SETTING, currentSetting, currentSettingValue }) =>
        this.getUpdatedState({ generalSetting, currentSetting, currentSettingValue })
    );

  handleSelectSetting = (selected?: NewCodePeriodSettingType) => this.setState({ selected });

  handleToggleSpecificSetting = (overrideGeneralSetting: boolean) =>
    this.setState({ overrideGeneralSetting });

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { component, branchLike } = this.props;
    const { analysis, days, selected: type, referenceBranch, overrideGeneralSetting } = this.state;

    if (!overrideGeneralSetting) {
      this.resetSetting();
      return;
    }

    const value = getSettingValue({ type, analysis, days, referenceBranch });

    if (type) {
      this.setState({ saving: true });
      setNewCodePeriod({
        project: component.key,
        branch: branchLike.name,
        type,
        value,
      }).then(
        () => {
          this.setState({
            saving: false,
            currentSetting: type,
            currentSettingValue: value || undefined,
            success: true,
          });
          this.resetSuccess();
        },
        () => {
          this.setState({ saving: false });
        }
      );
    }
  };

  render() {
    const { appState, component, branchLike } = this.props;
    const {
      analysis,
      branchList,
      currentSetting,
      days,
      generalSetting,
      loading,
      currentSettingValue,
      overrideGeneralSetting,
      referenceBranch,
      saving,
      selected,
      success,
    } = this.state;
    const branchSupportEnabled = this.props.hasFeature(Feature.BranchSupport);

    return (
      <>
        <Suggestions suggestions="project_baseline" />
        <div className="page page-limited">
          <AppHeader canAdmin={!!appState.canAdmin} />
          {loading ? (
            <DeferredSpinner />
          ) : (
            <div className="panel-white project-baseline">
              {branchSupportEnabled && <h2>{translate('project_baseline.default_setting')}</h2>}

              {generalSetting && overrideGeneralSetting !== undefined && (
                <ProjectBaselineSelector
                  analysis={analysis}
                  branch={branchLike}
                  branchList={branchList}
                  branchesEnabled={branchSupportEnabled}
                  component={component.key}
                  currentSetting={currentSetting}
                  currentSettingValue={currentSettingValue}
                  days={days}
                  generalSetting={generalSetting}
                  onCancel={this.handleCancel}
                  onSelectAnalysis={this.handleSelectAnalysis}
                  onSelectDays={this.handleSelectDays}
                  onSelectReferenceBranch={this.handleSelectReferenceBranch}
                  onSelectSetting={this.handleSelectSetting}
                  onSubmit={this.handleSubmit}
                  onToggleSpecificSetting={this.handleToggleSpecificSetting}
                  overrideGeneralSetting={overrideGeneralSetting}
                  referenceBranch={referenceBranch}
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
              {generalSetting && branchSupportEnabled && (
                <div className="huge-spacer-top branch-baseline-selector">
                  <hr />
                  <h2>{translate('project_baseline.configure_branches')}</h2>
                  <BranchList
                    branchList={branchList}
                    component={component}
                    inheritedSetting={
                      currentSetting
                        ? {
                            type: currentSetting,
                            value: currentSettingValue,
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

export default withComponentContext(withAvailableFeatures(withAppStateContext(App)));
