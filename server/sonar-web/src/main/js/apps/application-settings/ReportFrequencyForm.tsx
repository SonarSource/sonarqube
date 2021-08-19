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
import * as React from 'react';
import { Button, ResetButtonLink } from '../../components/controls/buttons';
import Select from '../../components/controls/Select';
import { translate } from '../../helpers/l10n';
import { sanitizeStringRestricted } from '../../helpers/sanitize';
import { SettingCategoryDefinition } from '../../types/settings';

export interface ReportFrequencyFormProps {
  definition: SettingCategoryDefinition;
  frequency: string;
  onSave: (value: string) => Promise<void>;
}

export default function ReportFrequencyForm(props: ReportFrequencyFormProps) {
  const { definition, frequency } = props;
  const { defaultValue } = definition;

  const [currentSelection, setCurrentSelection] = React.useState(frequency);

  const options = props.definition.options.map(option => ({
    label: option,
    value: option
  }));

  const handleReset = () => {
    if (defaultValue) {
      setCurrentSelection(defaultValue);
      props.onSave(defaultValue);
    }
  };

  return (
    <div>
      <h2>{translate('application_settings.report.frequency')}</h2>
      {definition.description && (
        <div
          className="markdown"
          // eslint-disable-next-line react/no-danger
          dangerouslySetInnerHTML={{
            __html: sanitizeStringRestricted(definition.description)
          }}
        />
      )}

      <Select
        className="input-medium"
        clearable={false}
        name={definition.name}
        onChange={({ value }: { value: string }) => setCurrentSelection(value)}
        options={options}
        value={currentSelection}
      />

      <div className="display-flex-center big-spacer-top">
        {frequency !== currentSelection && (
          <Button
            className="spacer-right button-success"
            onClick={() => props.onSave(currentSelection)}>
            {translate('save')}
          </Button>
        )}

        {defaultValue !== undefined && frequency !== defaultValue && (
          <Button className="spacer-right" onClick={handleReset}>
            {translate('reset_verb')}
          </Button>
        )}

        {frequency !== currentSelection && (
          <ResetButtonLink className="spacer-right" onClick={() => setCurrentSelection(frequency)}>
            {translate('cancel')}
          </ResetButtonLink>
        )}
      </div>
    </div>
  );
}
