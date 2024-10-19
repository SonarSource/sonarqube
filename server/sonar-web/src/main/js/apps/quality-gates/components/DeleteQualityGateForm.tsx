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
import { Modal } from 'design-system';
import * as React from 'react';
import { useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityGatesUrl } from '../../../helpers/urls';
import { useDeleteQualityGateMutation } from '../../../queries/quality-gates';
import { QualityGate } from '../../../types/types';

interface Props {
  organization: string;
  onClose: () => void;
  qualityGate: QualityGate;
}

export default function DeleteQualityGateForm({ qualityGate, onClose }: Readonly<Props>) {
  const { mutateAsync: deleteQualityGate } = useDeleteQualityGateMutation(qualityGate.name);
  const router = useRouter();

  const onDelete = async () => {
    await deleteQualityGate();
    router.push(getQualityGatesUrl());
  };

  return (
    <Modal
      headerTitle={translate('quality_gates.delete')}
      onClose={onClose}
      body={translateWithParameters('quality_gates.delete.confirm.message', qualityGate.name)}
      primaryButton={
        <Button hasAutoFocus type="submit" onClick={onDelete} variety={ButtonVariety.Danger}>
          {translate('delete')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
