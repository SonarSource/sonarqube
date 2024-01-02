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
import { DangerButtonPrimary, Modal } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { ParsedAnalysis } from '../../../../types/project-activity';

interface Props {
  analysis: ParsedAnalysis;
  deleteAnalysis: (analysis: string) => Promise<void>;
  onClose: () => void;
}

export default function RemoveAnalysisForm({ analysis, deleteAnalysis, onClose }: Props) {
  return (
    <Modal
      headerTitle={translate('project_activity.delete_analysis')}
      onClose={onClose}
      body={<p>{translate('project_activity.delete_analysis.question')}</p>}
      primaryButton={
        <DangerButtonPrimary onClick={() => deleteAnalysis(analysis.key)} type="submit">
          {translate('delete')}
        </DangerButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
