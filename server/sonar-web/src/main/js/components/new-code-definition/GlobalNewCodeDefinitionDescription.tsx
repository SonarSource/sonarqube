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
import { FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../types/new-code-definition';

interface Props {
  globalNcd: NewCodeDefinition;
  isGlobalNcdCompliant: boolean;
  canAdmin?: boolean;
}

export default function GlobalNewCodeDefinitionDescription({
  globalNcd,
  isGlobalNcdCompliant,
  canAdmin,
}: Props) {
  let setting: string;
  let description: string;
  let useCase: string;
  if (globalNcd.type === NewCodeDefinitionType.NumberOfDays) {
    setting = `${translate('new_code_definition.number_days')} (${translateWithParameters(
      'duration.days',
      globalNcd.value ?? '?'
    )})`;
    description = translate('new_code_definition.number_days.description');
    useCase = translate('new_code_definition.number_days.usecase');
  } else {
    setting = translate('new_code_definition.previous_version');
    description = translate('new_code_definition.previous_version.description');
    useCase = translate('new_code_definition.previous_version.usecase');
  }

  return (
    <>
      <div className="sw-flex sw-flex-col sw-gap-2 sw-max-w-[800px]">
        <strong className="sw-font-bold">{setting}</strong>
        {isGlobalNcdCompliant && (
          <>
            <span>{description}</span>
            <span>{useCase}</span>
          </>
        )}
      </div>
      {!isGlobalNcdCompliant && (
        <FlagMessage variant="warning" className="sw-mt-4 sw-max-w-[800px]">
          <span>
            <p className="sw-mb-2 sw-font-bold">
              {translate('new_code_definition.compliance.warning.title.global')}
            </p>
            <p className="sw-mb-2">
              {canAdmin ? (
                <FormattedMessage
                  id="new_code_definition.compliance.warning.explanation.admin"
                  defaultMessage={translate(
                    'new_code_definition.compliance.warning.explanation.admin'
                  )}
                  values={{
                    link: (
                      <Link to="/admin/settings?category=new_code_period">
                        {translate(
                          'new_code_definition.compliance.warning.explanation.action.admin.link'
                        )}
                      </Link>
                    ),
                  }}
                />
              ) : (
                translate('new_code_definition.compliance.warning.explanation')
              )}
            </p>
          </span>
        </FlagMessage>
      )}
    </>
  );
}
