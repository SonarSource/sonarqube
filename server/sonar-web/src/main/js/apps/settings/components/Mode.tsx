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

import {
  Button,
  ButtonGroup,
  ButtonVariety,
  Heading,
  Spinner,
  Text,
  TextSize,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { SelectionCard } from '~design-system';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { useSaveSimpleValueMutation, useStandardExperienceMode } from '../../../queries/settings';
import { SettingsKey } from '../../../types/settings';

export function Mode() {
  const { data: isStandardMode, isLoading } = useStandardExperienceMode();
  const { mutate: setMode, isPending } = useSaveSimpleValueMutation(true);
  const [changedMode, setChangedMode] = React.useState(false);
  const intl = useIntl();

  const handleSave = () => {
    // we need to invert because on BE we store isMQRMode
    setMode(
      { value: String(!!isStandardMode), key: SettingsKey.MQRMode },
      { onSuccess: () => setChangedMode(false) },
    );
  };

  return (
    <>
      <Heading as="h2" className="sw-mb-4">
        {intl.formatMessage({ id: 'settings.mode.title' })}
      </Heading>
      <Text>
        <FormattedMessage
          id="settings.mode.description.line1"
          values={{
            mqrLink: (
              <DocumentationLink to={DocLink.ModeMQR}>
                {intl.formatMessage({ id: 'settings.mode.mqr.name' })}
              </DocumentationLink>
            ),
            standardLink: (
              <DocumentationLink to={DocLink.ModeStandard}>
                {intl.formatMessage({ id: 'settings.mode.standard.name' })}
              </DocumentationLink>
            ),
          }}
        />
      </Text>
      <br />
      <br />
      <Text as="div" className="sw-max-w-full sw-mb-6">
        {intl.formatMessage({ id: 'settings.mode.description.line2' })}
      </Text>
      <Spinner isLoading={isLoading}>
        <div className="sw-flex sw-gap-6">
          <SelectionCard
            disabled={isPending}
            className="sw-basis-full"
            onClick={() => setChangedMode(isStandardMode === false)}
            selected={changedMode ? !isStandardMode : isStandardMode}
            title={intl.formatMessage({ id: 'settings.mode.standard.name' })}
          >
            <div>
              <Text>{intl.formatMessage({ id: 'settings.mode.standard.description.line1' })}</Text>
              <br />
              <br />
              <Text>{intl.formatMessage({ id: 'settings.mode.standard.description.line2' })}</Text>
            </div>
          </SelectionCard>
          <SelectionCard
            disabled={isPending}
            className="sw-basis-full"
            onClick={() => setChangedMode(isStandardMode === true)}
            selected={changedMode ? isStandardMode : !isStandardMode}
            title={intl.formatMessage({ id: 'settings.mode.mqr.name' })}
          >
            <div>
              <Text>{intl.formatMessage({ id: 'settings.mode.mqr.description.line1' })}</Text>
              <br />
              <br />
              <Text>{intl.formatMessage({ id: 'settings.mode.mqr.description.line2' })}</Text>
            </div>
          </SelectionCard>
        </div>
      </Spinner>
      <Text isSubdued as="div" className="sw-mt-6">
        <FormattedMessage id="settings.key_x" values={{ '0': SettingsKey.MQRMode }} />
      </Text>
      {changedMode && (
        <>
          <ButtonGroup className="sw-mt-6">
            <Button
              isDisabled={isPending}
              isLoading={isPending}
              aria-label={intl.formatMessage(
                { id: 'settings.mode.save' },
                { isStandardMode: !isStandardMode },
              )}
              onClick={handleSave}
              variety={ButtonVariety.Primary}
            >
              {intl.formatMessage({ id: 'save' })}
            </Button>

            <Button isDisabled={isPending} onClick={() => setChangedMode(false)}>
              {intl.formatMessage({ id: 'cancel' })}
            </Button>
          </ButtonGroup>
          <Text as="div" size={TextSize.Small} className="sw-mt-2">
            {intl.formatMessage({ id: 'settings.mode.save.warning' })}
          </Text>
        </>
      )}
    </>
  );
}
