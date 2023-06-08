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
import { RadioButton } from 'design-system/lib';
import { noop } from 'lodash';
import * as React from 'react';
import { getNewCodePeriod } from '../../api/newCodePeriod';
import { translate } from '../../helpers/l10n';
import {
  getNumberOfDaysDefaultValue,
  isNewCodeDefinitionCompliant,
} from '../../helpers/new-code-definition';
import {
  NewCodeDefinition,
  NewCodeDefinitionType,
  NewCodeDefinitiondWithCompliance,
} from '../../types/new-code-definition';
import RadioCard from '../controls/RadioCard';
import Tooltip from '../controls/Tooltip';
import { Alert } from '../ui/Alert';
import GlobalNewCodeDefinitionDescription from './GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionDaysOption from './NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from './NewCodeDefinitionPreviousVersionOption';

interface Props {
  canAdmin: boolean | undefined;
  onNcdChanged: (ncd: NewCodeDefinitiondWithCompliance) => void;
}

export default function NewCodeDefinitionSelector(props: Props) {
  const { canAdmin, onNcdChanged } = props;

  const [globalNcd, setGlobalNcd] = React.useState<NewCodeDefinition | null>(null);
  const [selectedNcdType, setSelectedNcdType] = React.useState<NewCodeDefinitionType | null>(null);
  const [days, setDays] = React.useState<string>('');

  const isGlobalNcdCompliant = React.useMemo(
    () => Boolean(globalNcd && isNewCodeDefinitionCompliant(globalNcd)),
    [globalNcd]
  );

  const initialNumberOfDays = React.useMemo(() => {
    const numberOfDays = getNumberOfDaysDefaultValue(globalNcd);
    setDays(numberOfDays);

    return numberOfDays;
  }, [globalNcd]);

  const isChanged = React.useMemo(
    () => selectedNcdType === NewCodeDefinitionType.NumberOfDays && days !== initialNumberOfDays,
    [selectedNcdType, days, initialNumberOfDays]
  );

  const isCompliant = React.useMemo(
    () =>
      !!selectedNcdType &&
      isNewCodeDefinitionCompliant({
        type: selectedNcdType,
        value: days,
      }),
    [selectedNcdType, days]
  );

  React.useEffect(() => {
    function fetchGlobalNcd() {
      getNewCodePeriod().then(setGlobalNcd, noop);
    }

    fetchGlobalNcd();
  }, []);

  React.useEffect(() => {
    if (selectedNcdType) {
      const type =
        selectedNcdType === NewCodeDefinitionType.Inherited ? undefined : selectedNcdType;
      const value = selectedNcdType === NewCodeDefinitionType.NumberOfDays ? days : undefined;
      onNcdChanged({ isCompliant, type, value });
    }
  }, [selectedNcdType, days, isCompliant, onNcdChanged]);

  return (
    <>
      <p className="sw-mt-10">
        <strong>{translate('new_code_definition.question')}</strong>
      </p>
      <div className="big-spacer-top spacer-bottom" role="radiogroup">
        <RadioButton
          aria-label={translate('new_code_definition.global_setting')}
          checked={selectedNcdType === NewCodeDefinitionType.Inherited}
          className="big-spacer-bottom"
          disabled={!isGlobalNcdCompliant}
          onCheck={() => setSelectedNcdType(NewCodeDefinitionType.Inherited)}
          value="general"
        >
          <Tooltip
            overlay={
              isGlobalNcdCompliant
                ? null
                : translate('new_code_definition.compliance.warning.title.global')
            }
          >
            <span>{translate('new_code_definition.global_setting')}</span>
          </Tooltip>
        </RadioButton>

        <div className="sw-ml-4">
          {globalNcd && (
            <GlobalNewCodeDefinitionDescription
              globalNcd={globalNcd}
              isGlobalNcdCompliant={isGlobalNcdCompliant}
              canAdmin={canAdmin}
            />
          )}
        </div>

        <RadioButton
          aria-label={translate('new_code_definition.specific_setting')}
          checked={Boolean(selectedNcdType && selectedNcdType !== NewCodeDefinitionType.Inherited)}
          className="huge-spacer-top"
          onCheck={() => setSelectedNcdType(NewCodeDefinitionType.PreviousVersion)}
          value="specific"
        >
          {translate('new_code_definition.specific_setting')}
        </RadioButton>
      </div>

      <div className="big-spacer-left big-spacer-right project-baseline-setting">
        <div className="display-flex-row big-spacer-bottom" role="radiogroup">
          <NewCodeDefinitionPreviousVersionOption
            disabled={Boolean(
              !selectedNcdType || selectedNcdType === NewCodeDefinitionType.Inherited
            )}
            onSelect={setSelectedNcdType}
            selected={selectedNcdType === NewCodeDefinitionType.PreviousVersion}
          />

          <NewCodeDefinitionDaysOption
            days={days}
            disabled={Boolean(
              !selectedNcdType || selectedNcdType === NewCodeDefinitionType.Inherited
            )}
            isChanged={isChanged}
            isValid={isCompliant}
            onChangeDays={setDays}
            onSelect={setSelectedNcdType}
            selected={selectedNcdType === NewCodeDefinitionType.NumberOfDays}
          />

          <RadioCard
            noRadio
            disabled={Boolean(
              !selectedNcdType || selectedNcdType === NewCodeDefinitionType.Inherited
            )}
            onClick={() => setSelectedNcdType(NewCodeDefinitionType.ReferenceBranch)}
            selected={selectedNcdType === NewCodeDefinitionType.ReferenceBranch}
            title={translate('new_code_definition.reference_branch')}
          >
            <div>
              <p className="sw-mb-3">
                {translate('new_code_definition.reference_branch.description')}
              </p>
              <p className="sw-mb-4">{translate('new_code_definition.reference_branch.usecase')}</p>
              {selectedNcdType === NewCodeDefinitionType.ReferenceBranch && (
                <Alert variant="info">
                  {translate('new_code_definition.reference_branch.notice')}
                </Alert>
              )}
            </div>
          </RadioCard>
        </div>
      </div>
    </>
  );
}
