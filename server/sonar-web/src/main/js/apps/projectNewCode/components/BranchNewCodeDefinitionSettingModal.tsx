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
import { noop } from 'lodash';
import * as React from 'react';
import { setNewCodeDefinition } from '../../../api/newCodeDefinition';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import NewCodeDefinitionDaysOption from '../../../components/new-code-definition/NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from '../../../components/new-code-definition/NewCodeDefinitionPreviousVersionOption';
import { NewCodeDefinitionLevels } from '../../../components/new-code-definition/utils';
import Spinner from '../../../components/ui/Spinner';
import { toISO8601WithOffsetString } from '../../../helpers/dates';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getNumberOfDaysDefaultValue } from '../../../helpers/new-code-definition';
import { Branch, BranchWithNewCodePeriod } from '../../../types/branch-like';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../../types/new-code-definition';
import { getSettingValue, validateSetting } from '../utils';
import NewCodeDefinitionSettingAnalysis from './NewCodeDefinitionSettingAnalysis';
import NewCodeDefinitionSettingReferenceBranch from './NewCodeDefinitionSettingReferenceBranch';

interface Props {
  branch: BranchWithNewCodePeriod;
  branchList: Branch[];
  component: string;
  onClose: (branch?: string, newSetting?: NewCodeDefinition) => void;
  inheritedSetting: NewCodeDefinition;
  globalNewCodeDefinition: NewCodeDefinition;
}

interface State {
  analysis: string;
  analysisDate?: Date;
  days: string;
  isChanged: boolean;
  referenceBranch: string;
  saving: boolean;
  selectedNewCodeDefinitionType?: NewCodeDefinitionType;
}

export default class BranchNewCodeDefinitionSettingModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    const { branch, branchList, inheritedSetting, globalNewCodeDefinition } = props;
    const otherBranches = branchList.filter((b) => b.name !== branch.name);
    const defaultBranch = otherBranches.length > 0 ? otherBranches[0].name : '';

    this.state = {
      analysis: this.getValueFromProps(NewCodeDefinitionType.SpecificAnalysis) || '',
      days:
        this.getValueFromProps(NewCodeDefinitionType.NumberOfDays) ||
        getNumberOfDaysDefaultValue(globalNewCodeDefinition, inheritedSetting),
      isChanged: false,
      referenceBranch:
        this.getValueFromProps(NewCodeDefinitionType.ReferenceBranch) || defaultBranch,
      saving: false,
      selectedNewCodeDefinitionType: branch.newCodePeriod?.type,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getValueFromProps(type: NewCodeDefinitionType) {
    return this.props.branch.newCodePeriod && this.props.branch.newCodePeriod.type === type
      ? this.props.branch.newCodePeriod.value
      : null;
  }

  branchToOption = (b: Branch) => ({
    label: b.name,
    value: b.name,
    isMain: b.isMain,
    isDisabled: b.name === this.props.branch.name, // cannot itself be used as a reference branch
  });

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { branch, component } = this.props;
    const {
      analysis,
      analysisDate,
      days,
      referenceBranch,
      selectedNewCodeDefinitionType: type,
    } = this.state;

    const value = getSettingValue({ type, analysis, numberOfDays: days, referenceBranch });

    if (type) {
      this.setState({ saving: true });
      setNewCodeDefinition({
        project: component,
        type,
        value,
        branch: branch.name,
      }).then(
        () => {
          if (this.mounted) {
            this.setState({
              saving: false,
              isChanged: false,
            });
            this.props.onClose(branch.name, {
              type,
              value,
              effectiveValue: analysisDate && toISO8601WithOffsetString(analysisDate),
            });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({
              saving: false,
            });
          }
        },
      );
    }
  };

  requestClose = () => this.props.onClose();

  handleSelectDays = (days: string) => this.setState({ days, isChanged: true });

  handleSelectReferenceBranch = (referenceBranch: string) =>
    this.setState({ referenceBranch, isChanged: true });

  handleSelectSetting = (selectedNewCodeDefinitionType: NewCodeDefinitionType) => {
    this.setState((currentState) => ({
      selectedNewCodeDefinitionType,
      isChanged: selectedNewCodeDefinitionType !== currentState.selectedNewCodeDefinitionType,
    }));
  };

  render() {
    const { branch, branchList } = this.props;
    const { analysis, days, isChanged, referenceBranch, saving, selectedNewCodeDefinitionType } =
      this.state;

    const header = translateWithParameters('baseline.new_code_period_for_branch_x', branch.name);

    const currentSetting = branch.newCodePeriod?.type;

    const isValid = validateSetting({
      numberOfDays: days,
      referenceBranch,
      selectedNewCodeDefinitionType,
    });

    return (
      <Modal contentLabel={header} onRequestClose={this.requestClose} size="large">
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body modal-container branch-baseline-setting-modal">
            <p className="sw-mb-3">{translate('baseline.new_code_period_for_branch_x.question')}</p>
            <div className="display-flex-column huge-spacer-bottom sw-gap-4" role="radiogroup">
              <NewCodeDefinitionPreviousVersionOption
                isDefault={false}
                onSelect={this.handleSelectSetting}
                selected={selectedNewCodeDefinitionType === NewCodeDefinitionType.PreviousVersion}
              />
              <NewCodeDefinitionDaysOption
                days={days}
                isChanged={isChanged}
                isValid={isValid}
                onChangeDays={this.handleSelectDays}
                onSelect={this.handleSelectSetting}
                selected={selectedNewCodeDefinitionType === NewCodeDefinitionType.NumberOfDays}
                settingLevel={NewCodeDefinitionLevels.Branch}
              />
              <NewCodeDefinitionSettingReferenceBranch
                branchList={branchList.map(this.branchToOption)}
                onChangeReferenceBranch={this.handleSelectReferenceBranch}
                onSelect={this.handleSelectSetting}
                referenceBranch={referenceBranch}
                selected={selectedNewCodeDefinitionType === NewCodeDefinitionType.ReferenceBranch}
                settingLevel={NewCodeDefinitionLevels.Branch}
              />
              {currentSetting === NewCodeDefinitionType.SpecificAnalysis && (
                <NewCodeDefinitionSettingAnalysis
                  onSelect={noop}
                  analysis={analysis}
                  branch={branch.name}
                  component={this.props.component}
                  selected={
                    selectedNewCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis
                  }
                />
              )}
            </div>
          </div>
          <footer className="modal-foot">
            <Spinner className="spacer-right" loading={saving} />
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
