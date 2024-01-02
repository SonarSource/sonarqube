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
import { FormattedMessage } from 'react-intl';
import { getNewCodePeriod, setNewCodePeriod } from '../../../api/newCodePeriod';
import DocLink from '../../../components/common/DocLink';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import AlertSuccessIcon from '../../../components/icons/AlertSuccessIcon';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { NewCodePeriodSettingType } from '../../../types/types';
import BaselineSettingDays from '../../projectBaseline/components/BaselineSettingDays';
import BaselineSettingPreviousVersion from '../../projectBaseline/components/BaselineSettingPreviousVersion';
import { validateDays } from '../../projectBaseline/utils';

interface State {
  currentSetting?: NewCodePeriodSettingType;
  days: string;
  loading: boolean;
  currentSettingValue?: string | number;
  saving: boolean;
  selected?: NewCodePeriodSettingType;
  success: boolean;
}

const DEFAULT_SETTING = 'PREVIOUS_VERSION';

export default class NewCodePeriod extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    loading: true,
    days: '30',
    saving: false,
    success: false,
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchNewCodePeriodSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchNewCodePeriodSetting() {
    getNewCodePeriod()
      .then(({ type, value }) => {
        const currentSetting = type || DEFAULT_SETTING;

        this.setState(({ days }) => ({
          currentSetting,
          days: currentSetting === 'NUMBER_OF_DAYS' ? String(value) : days,
          loading: false,
          currentSettingValue: value,
          selected: currentSetting,
        }));
      })
      .catch(() => {
        this.setState({ loading: false });
      });
  }

  onSelectDays = (days: string) => {
    this.setState({ days, success: false });
  };

  onSelectSetting = (selected: NewCodePeriodSettingType) => {
    this.setState({ selected, success: false });
  };

  onCancel = () => {
    this.setState(({ currentSetting, currentSettingValue, days }) => ({
      selected: currentSetting,
      days: currentSetting === 'NUMBER_OF_DAYS' ? String(currentSettingValue) : days,
    }));
  };

  onSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { days, selected } = this.state;

    const type = selected;
    const value = type === 'NUMBER_OF_DAYS' ? days : undefined;

    if (type) {
      this.setState({ saving: true, success: false });
      setNewCodePeriod({
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
        },
        () => {
          this.setState({
            saving: false,
          });
        }
      );
    }
  };

  render() {
    const { currentSetting, days, loading, currentSettingValue, saving, selected, success } =
      this.state;

    const isChanged =
      selected !== currentSetting ||
      (selected === 'NUMBER_OF_DAYS' && String(days) !== currentSettingValue);

    const isValid = selected !== 'NUMBER_OF_DAYS' || validateDays(days);

    return (
      <ul className="settings-sub-categories-list">
        <li>
          <ul className="settings-definitions-list">
            <li>
              <div className="settings-definition">
                <div className="settings-definition-left">
                  <h3
                    className="settings-definition-name"
                    title={translate('settings.new_code_period.title')}
                  >
                    {translate('settings.new_code_period.title')}
                  </h3>

                  <div className="small big-spacer-top">
                    <FormattedMessage
                      defaultMessage={translate('settings.new_code_period.description')}
                      id="settings.new_code_period.description"
                      values={{
                        link: (
                          <DocLink to="/project-administration/defining-new-code/">
                            {translate('learn_more')}
                          </DocLink>
                        ),
                      }}
                    />
                    <p className="spacer-top">
                      {translate('settings.new_code_period.description2')}
                    </p>
                  </div>
                </div>

                <div className="settings-definition-right">
                  {loading ? (
                    <DeferredSpinner />
                  ) : (
                    <form onSubmit={this.onSubmit}>
                      <BaselineSettingPreviousVersion
                        isDefault={true}
                        onSelect={this.onSelectSetting}
                        selected={selected === 'PREVIOUS_VERSION'}
                      />
                      <BaselineSettingDays
                        className="spacer-top"
                        days={days}
                        isChanged={isChanged}
                        isValid={isValid}
                        onChangeDays={this.onSelectDays}
                        onSelect={this.onSelectSetting}
                        selected={selected === 'NUMBER_OF_DAYS'}
                      />
                      {isChanged && (
                        <div className="big-spacer-top">
                          <p className="spacer-bottom">
                            {translate('baseline.next_analysis_notice')}
                          </p>
                          <DeferredSpinner className="spacer-right" loading={saving} />
                          <SubmitButton disabled={saving || !isValid}>
                            {translate('save')}
                          </SubmitButton>
                          <ResetButtonLink className="spacer-left" onClick={this.onCancel}>
                            {translate('cancel')}
                          </ResetButtonLink>
                        </div>
                      )}
                      {!saving && !loading && success && (
                        <div className="big-spacer-top">
                          <span className="text-success">
                            <AlertSuccessIcon className="spacer-right" />
                            {translate('settings.state.saved')}
                          </span>
                        </div>
                      )}
                    </form>
                  )}
                </div>
              </div>
            </li>
          </ul>
        </li>
      </ul>
    );
  }
}
