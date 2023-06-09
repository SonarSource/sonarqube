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
import { useEffect } from 'react';
import { getNewCodePeriod } from '../../api/newCodePeriod';
import { translate } from '../../helpers/l10n';
import { isNewCodeDefinitionCompliant } from '../../helpers/periods';
import {
  NewCodePeriod,
  NewCodePeriodSettingType,
  NewCodePeriodWithCompliance,
} from '../../types/types';
import Radio from '../controls/Radio';
import RadioCard from '../controls/RadioCard';
import Tooltip from '../controls/Tooltip';
import { Alert } from '../ui/Alert';
import GlobalNewCodeDefinitionDescription from './GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionDaysOption from './NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from './NewCodeDefinitionPreviousVersionOption';

interface Props {
  canAdmin: boolean | undefined;
  onNcdChanged: (ncd: NewCodePeriodWithCompliance) => void;
}

const INITIAL_DAYS = '30';

export default function NewCodeDefinitionSelector(props: Props) {
  const { canAdmin, onNcdChanged } = props;

  const [globalNcd, setGlobalNcd] = React.useState<NewCodePeriod | null>(null);
  const [selectedNcdType, setSelectedNcdType] = React.useState<NewCodePeriodSettingType | null>(
    null
  );
  const [days, setDays] = React.useState<string>(INITIAL_DAYS);

  const iGlobalNcdCompliant = React.useMemo(
    () => Boolean(globalNcd && isNewCodeDefinitionCompliant(globalNcd)),
    [globalNcd]
  );

  const isChanged = React.useMemo(
    () => selectedNcdType === NewCodePeriodSettingType.NUMBER_OF_DAYS && days !== INITIAL_DAYS,
    [selectedNcdType, days]
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

  useEffect(() => {
    function fetchGlobalNcd() {
      getNewCodePeriod().then(setGlobalNcd, noop);
    }

    fetchGlobalNcd();
  }, []);

  useEffect(() => {
    if (selectedNcdType) {
      const type =
        selectedNcdType === NewCodePeriodSettingType.INHERITED ? undefined : selectedNcdType;
      const value = selectedNcdType === NewCodePeriodSettingType.NUMBER_OF_DAYS ? days : undefined;
      onNcdChanged({ isCompliant, type, value });
    }
  }, [selectedNcdType, days, isCompliant, onNcdChanged]);

  return (
    <>
      <p className="sw-mt-10">
        <strong>{translate('new_code_definition.question')}</strong>
      </p>
      <div className="big-spacer-top spacer-bottom" role="radiogroup">
        <Radio
          ariaLabel={translate('new_code_definition.global_setting')}
          checked={selectedNcdType === NewCodePeriodSettingType.INHERITED}
          className="big-spacer-bottom"
          disabled={!iGlobalNcdCompliant}
          onCheck={() => setSelectedNcdType(NewCodePeriodSettingType.INHERITED)}
          value="general"
        >
          <Tooltip
            overlay={
              iGlobalNcdCompliant
                ? null
                : translate('new_code_definition.compliance.warning.title.global')
            }
          >
            <span>{translate('new_code_definition.global_setting')}</span>
          </Tooltip>
        </Radio>

        <div className="sw-ml-4">
          {globalNcd && (
            <GlobalNewCodeDefinitionDescription
              globalNcd={globalNcd}
              isGlobalNcdCompliant={iGlobalNcdCompliant}
              canAdmin={canAdmin}
            />
          )}
        </div>

        <Radio
          ariaLabel={translate('new_code_definition.specific_setting')}
          checked={Boolean(
            selectedNcdType && selectedNcdType !== NewCodePeriodSettingType.INHERITED
          )}
          className="huge-spacer-top"
          onCheck={() => setSelectedNcdType(NewCodePeriodSettingType.PREVIOUS_VERSION)}
          value="specific"
        >
          {translate('new_code_definition.specific_setting')}
        </Radio>
      </div>

      <div className="big-spacer-left big-spacer-right project-baseline-setting">
        <div className="display-flex-row big-spacer-bottom" role="radiogroup">
          <NewCodeDefinitionPreviousVersionOption
            disabled={Boolean(
              !selectedNcdType || selectedNcdType === NewCodePeriodSettingType.INHERITED
            )}
            onSelect={setSelectedNcdType}
            selected={selectedNcdType === NewCodePeriodSettingType.PREVIOUS_VERSION}
          />

          <NewCodeDefinitionDaysOption
            days={days}
            disabled={Boolean(
              !selectedNcdType || selectedNcdType === NewCodePeriodSettingType.INHERITED
            )}
            isChanged={isChanged}
            isValid={isCompliant}
            onChangeDays={setDays}
            onSelect={setSelectedNcdType}
            selected={selectedNcdType === NewCodePeriodSettingType.NUMBER_OF_DAYS}
          />

          <RadioCard
            disabled={Boolean(
              !selectedNcdType || selectedNcdType === NewCodePeriodSettingType.INHERITED
            )}
            onClick={() => setSelectedNcdType(NewCodePeriodSettingType.REFERENCE_BRANCH)}
            selected={selectedNcdType === NewCodePeriodSettingType.REFERENCE_BRANCH}
            title={translate('new_code_definition.reference_branch')}
          >
            <div>
              <p className="sw-mb-3">
                {translate('new_code_definition.reference_branch.description')}
              </p>
              <p className="sw-mb-4">{translate('new_code_definition.reference_branch.usecase')}</p>
              {selectedNcdType === NewCodePeriodSettingType.REFERENCE_BRANCH && (
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
