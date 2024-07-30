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
import { Spinner } from '@sonarsource/echoes-react';
import { ToggleButton } from 'design-system';
import * as React from 'react';
import { Location } from '~sonar-aligned/types/router';
import withKeyboardNavigation from '../../../components/hoc/withKeyboardNavigation';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import SourceViewerPreview from '../../../components/SourceViewer/SourceViewerPreview';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Measure } from '../../../types/types';

export interface SourceViewerWrapperProps {
  branchLike?: BranchLike;
  component: string;
  componentMeasures: Measure[] | undefined;
  location: Location;
}

const PREVIEW_MODE_SUPPORTED_EXTENSIONS = ['ipynb'];

function SourceViewerWrapper(props: SourceViewerWrapperProps) {
  const { branchLike, component, componentMeasures, location } = props;

  const isPreviewSupported = React.useMemo(
    () => PREVIEW_MODE_SUPPORTED_EXTENSIONS.includes(component.split('.').pop() ?? ''),
    [component],
  );

  const [tab, setTab] = React.useState('preview');
  const [isLoading, setIsLoading] = React.useState(false);

  const { line } = location.query;
  const finalLine = line ? Number(line) : undefined;

  const handleLoaded = React.useCallback(() => {
    setIsLoading(false);
    if (line) {
      const row = document.querySelector(`.it__source-line-code[data-line-number="${line}"]`);
      if (row) {
        row.scrollIntoView({ block: 'center' });
      }
    }
  }, [line]);

  const handleTabChange = React.useCallback((value: string) => {
    setTab(value);
    if (value === 'code') {
      setIsLoading(true);
    }
  }, []);

  return isPreviewSupported ? (
    <>
      <div className="sw-mb-4">
        <ToggleButton
          options={[
            { label: translate('preview'), value: 'preview' },
            { label: translate('code'), value: 'code' },
          ]}
          value={tab}
          onChange={(value) => handleTabChange(value)}
        />
      </div>
      {tab === 'preview' ? (
        <SourceViewerPreview branchLike={branchLike} component={component} />
      ) : (
        <>
          <SourceViewer
            aroundLine={finalLine}
            branchLike={branchLike}
            component={component}
            componentMeasures={componentMeasures}
            highlightedLine={finalLine}
            onLoaded={handleLoaded}
            showMeasures
          />
          <Spinner isLoading={isLoading} />
        </>
      )}
    </>
  ) : (
    <SourceViewer
      aroundLine={finalLine}
      branchLike={branchLike}
      component={component}
      componentMeasures={componentMeasures}
      highlightedLine={finalLine}
      onLoaded={handleLoaded}
      showMeasures
    />
  );
}

export default withKeyboardNavigation(SourceViewerWrapper);
