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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getEdition, getEditionUrl } from '../../../../helpers/editions';
import {
  AlmBindingDefinition,
  AlmKeys,
  AlmSettingsBindingStatus
} from '../../../../types/alm-settings';
import { EditionKey } from '../../../../types/editions';
import AlmBindingDefinitionBox from './AlmBindingDefinitionBox';
import AlmBindingDefinitionForm, {
  AlmBindingDefinitionFormChildrenProps
} from './AlmBindingDefinitionForm';

export interface AlmTabRendererProps<B> {
  alm: AlmKeys;
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  editedDefinition?: B;
  defaultBinding: B;
  definitions: B[];
  form: (props: AlmBindingDefinitionFormChildrenProps<B>) => React.ReactNode;
  help?: React.ReactNode;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onCheck: (definitionKey: string) => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSubmit: (config: B, originalKey: string) => void;
  optionalFields?: Array<keyof B>;
  submitting: boolean;
  success: boolean;
}

const renderDefaultHelp = (alm: AlmKeys) => (
  <FormattedMessage
    defaultMessage={translate(`settings.almintegration.${alm}.info`)}
    id={`settings.almintegration.${alm}.info`}
    values={{
      link: (
        <Link target="_blank" to="/documentation/analysis/pr-decoration/">
          {translate('learn_more')}
        </Link>
      )
    }}
  />
);

export default function AlmTabRenderer<B extends AlmBindingDefinition>(
  props: AlmTabRendererProps<B>
) {
  const {
    alm,
    definitions,
    definitionStatus,
    editedDefinition,
    form,
    loadingAlmDefinitions,
    loadingProjectCount,
    multipleAlmEnabled,
    optionalFields,
    help = renderDefaultHelp(alm)
  } = props;

  const preventCreation = loadingProjectCount || (!multipleAlmEnabled && definitions.length > 0);
  const creationTooltip = preventCreation ? (
    <FormattedMessage
      id="settings.almintegration.create.tooltip"
      defaultMessage={translate('settings.almintegration.create.tooltip')}
      values={{
        link: (
          <a
            href={getEditionUrl(getEdition(EditionKey.enterprise), {
              sourceEdition: EditionKey.developer
            })}
            rel="noopener noreferrer"
            target="_blank">
            {translate('settings.almintegration.create.tooltip.link')}
          </a>
        ),
        alm: translate('alm', alm)
      }}
    />
  ) : null;

  return (
    <div className="big-padded">
      <DeferredSpinner loading={loadingAlmDefinitions}>
        {definitions.length === 0 && (
          <p className="spacer-top">{translate('settings.almintegration.empty', alm)}</p>
        )}

        <div className={definitions.length > 0 ? 'spacer-bottom text-right' : 'big-spacer-top'}>
          <Tooltip overlay={creationTooltip} mouseLeaveDelay={0.25}>
            <Button
              data-test="settings__alm-create"
              disabled={preventCreation}
              onClick={props.onCreate}>
              {translate('settings.almintegration.create')}
            </Button>
          </Tooltip>
        </div>
        {definitions.map(def => (
          <AlmBindingDefinitionBox
            alm={alm}
            definition={def}
            key={def.key}
            multipleDefinitions={definitions.length > 1}
            onCheck={props.onCheck}
            onDelete={props.onDelete}
            onEdit={props.onEdit}
            status={definitionStatus[def.key]}
          />
        ))}

        {editedDefinition && (
          <AlmBindingDefinitionForm
            bindingDefinition={editedDefinition}
            help={help}
            isSecondInstance={definitions.length === 1}
            onCancel={props.onCancel}
            onSubmit={props.onSubmit}
            optionalFields={optionalFields}>
            {form}
          </AlmBindingDefinitionForm>
        )}
      </DeferredSpinner>
    </div>
  );
}
