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
import { Modal } from '~design-system';
import { translate } from '../../../../helpers/l10n';
import { useDeleteEventMutation } from '../../../../queries/project-analyses';
import { AnalysisEvent } from '../../../../types/project-activity';

export interface RemoveEventFormProps {
  analysisKey: string;
  event: AnalysisEvent;
  header: string;
  onClose: () => void;
  removeEventQuestion: string;
}

export default function RemoveEventForm(props: RemoveEventFormProps) {
  const { analysisKey, event, header, removeEventQuestion } = props;

  const { mutate: deleteEvent } = useDeleteEventMutation();
  return (
    <Modal
      headerTitle={header}
      onClose={props.onClose}
      body={<p>{removeEventQuestion}</p>}
      primaryButton={
        <Button
          onClick={() => deleteEvent({ analysis: analysisKey, event: event.key })}
          variety={ButtonVariety.Danger}
        >
          {translate('delete')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
