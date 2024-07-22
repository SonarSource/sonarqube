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
import { Button } from '@sonarsource/echoes-react';
import { FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import { useRouter } from '~sonar-aligned/components/hoc/withRouter';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { useCreateQualityGateMutation } from '../../../queries/quality-gates';

interface Props {
  onClose: () => void;
}

export default function CreateQualityGateForm({ onClose }: Readonly<Props>) {
  const [name, setName] = React.useState('');
  const { mutateAsync: createQualityGate } = useCreateQualityGateMutation();
  const router = useRouter();

  const handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    setName(event.currentTarget.value);
  };

  const handleCreate = async () => {
    if (name !== undefined) {
      const qualityGate = await createQualityGate(name);
      onClose();
      router.push(getQualityGateUrl(qualityGate.name));
    }
  };

  const handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    handleCreate();
  };

  const body = (
    <form onSubmit={handleFormSubmit}>
      <MandatoryFieldsExplanation className="modal-field" />
      <FormField
        htmlFor="quality-gate-form-name"
        label={translate('name')}
        required
        requiredAriaLabel={translate('field_required')}
      >
        <InputField
          className="sw-mb-1"
          autoComplete="off"
          id="quality-gate-form-name"
          maxLength={256}
          name="key"
          onChange={handleNameChange}
          type="text"
          size="full"
          value={name}
        />
      </FormField>
    </form>
  );

  return (
    <Modal
      onClose={onClose}
      headerTitle={translate('quality_gates.create')}
      isScrollable
      body={body}
      primaryButton={
        <Button
          isDisabled={name === null || name === ''}
          form="create-application-form"
          type="submit"
          onClick={handleCreate}
        >
          {translate('quality_gate.create')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
