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
import { ButtonPrimary, InputField, Modal } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { useCreateEventMutation } from '../../../../queries/project-analyses';
import { ParsedAnalysis } from '../../../../types/project-activity';

interface Props {
  addEventButtonText: string;
  analysis: ParsedAnalysis;
  category?: string;
  onClose: () => void;
}

export default function AddEventForm(props: Readonly<Props>) {
  const { addEventButtonText, onClose, analysis, category } = props;
  const [name, setName] = React.useState('');
  const { mutate: createEvent } = useCreateEventMutation(onClose);

  const handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setName(event.target.value);
  };

  const handleSubmit = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    const data: Parameters<typeof createEvent>[0] = { analysis: analysis.key, name };

    if (category !== undefined) {
      data.category = category;
    }
    createEvent(data);
  };

  return (
    <Modal
      headerTitle={translate(addEventButtonText)}
      onClose={onClose}
      body={
        <form id="add-event-form">
          <label htmlFor="name">{translate('name')}</label>
          <InputField
            id="name"
            className="sw-my-2"
            autoFocus
            onChange={handleNameChange}
            type="text"
            value={name}
            size="full"
          />
        </form>
      }
      primaryButton={
        <ButtonPrimary
          id="add-event-submit"
          form="add-event-form"
          type="submit"
          disabled={name === ''}
          onClick={handleSubmit}
        >
          {translate('save')}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
