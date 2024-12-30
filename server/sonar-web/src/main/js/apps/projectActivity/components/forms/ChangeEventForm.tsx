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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import { InputField, Modal } from '~design-system';
import { translate } from '../../../../helpers/l10n';
import { useChangeEventMutation } from '../../../../queries/project-analyses';
import { AnalysisEvent } from '../../../../types/project-activity';

interface Props {
  event: AnalysisEvent;
  header: string;
  onClose: () => void;
}

export default function ChangeEventForm(props: Readonly<Props>) {
  const { event, header, onClose } = props;
  const [name, setName] = React.useState(event.name);

  const { mutate: changeEvent } = useChangeEventMutation(onClose);

  const changeInput = (event: React.ChangeEvent<HTMLInputElement>) => {
    setName(event.target.value);
  };

  const handleSubmit = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    changeEvent({ event: event.key, name });
  };

  return (
    <Modal
      headerTitle={header}
      onClose={onClose}
      body={
        <form id="change-event-form">
          <label htmlFor="name">{translate('name')}</label>
          <InputField
            id="name"
            className="sw-my-2"
            autoFocus
            onChange={changeInput}
            type="text"
            value={name}
            size="full"
          />
        </form>
      }
      primaryButton={
        <Button
          id="change-event-submit"
          form="change-event-form"
          type="submit"
          isDisabled={name === '' || name === event.name}
          onClick={handleSubmit}
          variety={ButtonVariety.Primary}
        >
          {translate('change_verb')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
