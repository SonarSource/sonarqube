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
import { FormField, InputField, Modal } from '~design-system';
import { useRouter } from '~sonar-aligned/components/hoc/withRouter';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { useCopyQualityGateMutation } from '../../../queries/quality-gates';
import { QualityGate } from '../../../types/types';

interface Props {
  onClose: () => void;
  qualityGate: QualityGate;
}

const FORM_ID = 'rename-quality-gate';

export default function CopyQualityGateForm({ qualityGate, onClose }: Readonly<Props>) {
  const [name, setName] = React.useState(qualityGate.name);
  const { mutateAsync: copyQualityGate } = useCopyQualityGateMutation(qualityGate.name);
  const router = useRouter();

  const handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setName(event.currentTarget.value);
  };

  const handleCopy = async (event: React.FormEvent) => {
    event.preventDefault();

    const newQualityGate = await copyQualityGate(name);
    router.push(getQualityGateUrl(newQualityGate.name));
  };

  const buttonDisabled = !name || (qualityGate && qualityGate.name === name);

  return (
    <Modal
      headerTitle={translate('quality_gates.copy')}
      onClose={onClose}
      body={
        <form id={FORM_ID} onSubmit={handleCopy}>
          <MandatoryFieldsExplanation />
          <FormField
            label={translate('name')}
            htmlFor="quality-gate-form-name"
            required
            className="sw-my-2"
          >
            <InputField
              autoFocus
              id="quality-gate-form-name"
              maxLength={100}
              onChange={handleNameChange}
              size="auto"
              type="text"
              value={name}
            />
          </FormField>
        </form>
      }
      primaryButton={
        <Button
          hasAutoFocus
          type="submit"
          isDisabled={buttonDisabled}
          form={FORM_ID}
          variety={ButtonVariety.Primary}
        >
          {translate('copy')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
