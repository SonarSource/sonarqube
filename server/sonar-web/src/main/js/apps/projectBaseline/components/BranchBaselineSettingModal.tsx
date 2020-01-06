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
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { toNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { setNewCodePeriod } from '../../../api/newCodePeriod';
import { BranchWithNewCodePeriod } from '../../../types/branch-like';
import { getSettingValue, validateSetting } from '../utils';
import BaselineSettingAnalysis from './BaselineSettingAnalysis';
import BaselineSettingDays from './BaselineSettingDays';
import BaselineSettingPreviousVersion from './BaselineSettingPreviousVersion';
import BranchAnalysisList from './BranchAnalysisList';

interface Props {
  branch: BranchWithNewCodePeriod;
  component: string;
  onClose: (branch?: string, newSetting?: T.NewCodePeriod) => void;
}

interface State {
  analysis: string;
  analysisDate?: Date;
  days: string;
  saving: boolean;
  selected?: T.NewCodePeriodSettingType;
}

export default class BranchBaselineSettingModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      analysis: this.getValueFromProps('SPECIFIC_ANALYSIS') || '',
      days: this.getValueFromProps('NUMBER_OF_DAYS') || '30',
      saving: false,
      selected: this.props.branch.newCodePeriod && this.props.branch.newCodePeriod.type
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getValueFromProps(type: T.NewCodePeriodSettingType) {
    return this.props.branch.newCodePeriod && this.props.branch.newCodePeriod.type === type
      ? this.props.branch.newCodePeriod.value
      : null;
  }

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { branch, component } = this.props;
    const { analysis, analysisDate, days, selected: type } = this.state;

    const value = getSettingValue({ type, analysis, days });

    if (type) {
      this.setState({ saving: true });
      setNewCodePeriod({
        project: component,
        type,
        value,
        branch: branch.name
      }).then(
        () => {
          this.setState({
            saving: false
          });
          this.props.onClose(branch.name, {
            type,
            value,
            effectiveValue: analysisDate && toNotSoISOString(analysisDate)
          });
        },
        () => {
          this.setState({
            saving: false
          });
        }
      );
    }
  };

  requestClose = () => this.props.onClose();

  handleSelectAnalysis = (analysis: T.ParsedAnalysis) =>
    this.setState({ analysis: analysis.key, analysisDate: analysis.date });

  handleSelectDays = (days: string) => this.setState({ days });

  handleSelectSetting = (selected: T.NewCodePeriodSettingType) => this.setState({ selected });

  render() {
    const { branch } = this.props;
    const { analysis, days, saving, selected } = this.state;

    const header = translateWithParameters('baseline.new_code_period_for_branch_x', branch.name);

    const currentSetting = branch.newCodePeriod && branch.newCodePeriod.type;
    const currentSettingValue = branch.newCodePeriod && branch.newCodePeriod.value;

    const { isChanged, isValid } = validateSetting({
      analysis,
      currentSetting,
      currentSettingValue,
      days,
      selected
    });

    return (
      <Modal contentLabel={header} onRequestClose={this.requestClose} size="large">
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body modal-container branch-baseline-setting-modal">
            <div className="display-flex-row huge-spacer-bottom" role="radiogroup">
              <BaselineSettingPreviousVersion
                isDefault={false}
                onSelect={this.handleSelectSetting}
                selected={selected === 'PREVIOUS_VERSION'}
              />
              <BaselineSettingDays
                days={days}
                isChanged={isChanged}
                isValid={isValid}
                onChangeDays={this.handleSelectDays}
                onSelect={this.handleSelectSetting}
                selected={selected === 'NUMBER_OF_DAYS'}
              />
              <BaselineSettingAnalysis
                onSelect={this.handleSelectSetting}
                selected={selected === 'SPECIFIC_ANALYSIS'}
              />
            </div>
            {selected === 'SPECIFIC_ANALYSIS' && (
              <BranchAnalysisList
                analysis={analysis}
                branch={branch.name}
                component={this.props.component}
                onSelectAnalysis={this.handleSelectAnalysis}
              />
            )}
          </div>
          <footer className="modal-foot">
            <DeferredSpinner className="spacer-right" loading={saving} />
            <SubmitButton disabled={!isChanged || saving || !isValid}>
              {translate('save')}
            </SubmitButton>
            <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
