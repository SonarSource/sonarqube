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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getNewCodePeriod, resetNewCodePeriod, setNewCodePeriod } from '../../../api/newCodePeriod';
import '../styles.css';
import BranchList from './BranchList';
import ProjectBaselineSelector from './ProjectBaselineSelector';

interface Props {
  branchLikes: T.BranchLike[];
  canAdmin?: boolean;
  component: T.Component;
}

interface State {
  currentSetting?: T.NewCodePeriodSettingType;
  currentSettingValue?: string | number;
  days: string;
  generalSetting?: { type: T.NewCodePeriodSettingType; value?: string };
  loading: boolean;
  saving: boolean;
  selected?: T.NewCodePeriodSettingType;
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

  componentDidMount() {
    this.mounted = true;
    this.fetchLeakPeriodSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLeakPeriodSetting() {
    this.setState({ loading: true });
    Promise.all([getNewCodePeriod(), getNewCodePeriod({ project: this.props.component.key })]).then(
      ([generalSetting, setting]) => {
        if (this.mounted) {
          if (!generalSetting.type) {
            generalSetting = DEFAULT_GENERAL_SETTING;
          }
          const currentSettingValue = setting.value;
          const currentSetting = setting.inherited ? undefined : setting.type || 'PREVIOUS_VERSION';
          const newState = {
            loading: false,
            currentSetting,
            currentSettingValue,
            generalSetting,
            selected: currentSetting
          };

          if (currentSetting === 'NUMBER_OF_DAYS') {
            this.setState({
              days: currentSettingValue || '30',
              ...newState
            });
          } else {
            this.setState(newState);
          }
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
          selected: undefined
        });
      },
      () => {
        this.setState({ saving: false });
      }
    );
  };

  handleSelectDays = (days: string) => this.setState({ days });

  handleSelectSetting = (selected?: T.NewCodePeriodSettingType) => this.setState({ selected });

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { component } = this.props;
    const { days, selected } = this.state;

    const type = selected;
    const value = type === 'NUMBER_OF_DAYS' ? days : null;

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
            currentSettingValue: value || undefined
          });
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
                <Link to="/documentation/user-guide/fixing-the-water-leak/">
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

  renderGeneralSetting(generalSetting: { type: T.NewCodePeriodSettingType; value?: string }) {
    if (generalSetting.type === 'NUMBER_OF_DAYS') {
      return `${translate('baseline.number_days')} (${translateWithParameters(
        'duration.days',
        generalSetting.value || '?'
      )})`;
    } else {
      return translate('baseline.previous_version');
    }
  }

  render() {
    const {
      currentSetting,
      days,
      generalSetting,
      loading,
      currentSettingValue,
      saving,
      selected
    } = this.state;

    return (
      <div className="page page-limited">
        {this.renderHeader()}
        {loading ? (
          <DeferredSpinner />
        ) : (
          <div className="panel panel-white">
            <h2>{translate('project_baseline.default_setting')}</h2>
            <p>{translate('project_baseline.default_setting.description')}</p>

            {generalSetting && (
              <div className="text-right spacer-bottom">
                {currentSetting && (
                  <>
                    <Button
                      className="spacer-right little-spacer-bottom"
                      onClick={this.resetSetting}>
                      {translate('project_baseline.reset_to_general')}
                    </Button>
                  </>
                )}
                <div className="spacer-top spacer-right medium">
                  <strong>{translate('project_baseline.general_setting')}: </strong>
                  {this.renderGeneralSetting(generalSetting)}
                </div>
              </div>
            )}

            <ProjectBaselineSelector
              currentSetting={currentSetting}
              currentSettingValue={currentSettingValue}
              days={days}
              generalSetting={generalSetting}
              onSelectDays={this.handleSelectDays}
              onSelectSetting={this.handleSelectSetting}
              onSubmit={this.handleSubmit}
              saving={saving}
              selected={selected}
            />

            <BranchList branchLikes={this.props.branchLikes} component={this.props.component} />
          </div>
        )}
      </div>
    );
  }
}
