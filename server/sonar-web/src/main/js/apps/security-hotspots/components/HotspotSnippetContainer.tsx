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
import * as React from 'react';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { getSources } from '../../../api/components';
import { locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import { Component, ExpandDirection, FlowLocation, SourceLine } from '../../../types/types';
import { constructSourceViewerFile, getLocations } from '../utils';
import HotspotSnippetContainerRenderer from './HotspotSnippetContainerRenderer';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  hotspot: Hotspot;
  onLocationSelect: (index: number) => void;
  selectedHotspotLocation?: number;
}

interface State {
  highlightedSymbols: string[];
  lastLine?: number;
  loading: boolean;
  secondaryLocations: FlowLocation[];
  sourceLines: SourceLine[];
}

const BUFFER_LINES = 10;
const EXPAND_BY_LINES = 50;

export default class HotspotSnippetContainer extends React.Component<Props, State> {
  mounted = false;
  state: State = {
    highlightedSymbols: [],
    loading: true,
    sourceLines: [],
    secondaryLocations: [],
  };

  async componentDidMount() {
    this.mounted = true;
    await this.initializeSecondaryLocations();
    this.fetchSources();
  }

  async componentDidUpdate(prevProps: Props) {
    if (prevProps.hotspot.key !== this.props.hotspot.key) {
      await this.initializeSecondaryLocations();
      this.fetchSources();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkLastLine(lines: SourceLine[], target: number): number | undefined {
    if (lines.length < 1) {
      return undefined;
    }
    const lastLineReceived = lines[lines.length - 1].line;
    if (lastLineReceived < target) {
      return lastLineReceived;
    }
    return undefined;
  }

  async fetchSources() {
    const {
      branchLike,
      hotspot: { component, textRange },
    } = this.props;

    const { secondaryLocations } = this.state;

    if (!textRange) {
      // Hotspot not associated to any loc
      this.setState({ loading: false, lastLine: undefined, sourceLines: [] });
      return;
    }

    // Search for the min startLine within primary and secondary locations
    const from = Math.max(
      1,
      Math.min(
        ...[textRange, ...secondaryLocations.map((l) => l.textRange)].map(
          (t) => t.startLine - BUFFER_LINES,
        ),
      ),
    );
    // Search for the max endLine within primary and secondary locations
    const to = Math.max(
      ...[textRange, ...secondaryLocations.map((l) => l.textRange)].map(
        // Add 1 to check for end-of-file
        (t) => t.endLine + BUFFER_LINES + 1,
      ),
    );

    this.setState({ loading: true });

    let sourceLines = await getSources({
      key: component.key,
      from,
      to,
      ...getBranchLikeQuery(branchLike),
    }).catch(() => [] as SourceLine[]);

    if (this.mounted) {
      const lastLine = this.checkLastLine(sourceLines, to);

      // remove extra sourceline if we didn't reach the end:
      sourceLines = lastLine ? sourceLines : sourceLines.slice(0, -1);
      this.setState({ lastLine, loading: false, sourceLines });
    }
  }

  initializeSecondaryLocations() {
    const { hotspot } = this.props;

    return new Promise((resolve) => {
      this.setState(
        {
          secondaryLocations: getLocations(hotspot.flows, undefined).map((location, index) => ({
            ...location,
            index,
            text: location.msg,
          })),
        },
        () => resolve(undefined),
      );
    });
  }

  handleExpansion = (direction: ExpandDirection) => {
    const { branchLike, hotspot } = this.props;
    const { sourceLines } = this.state;

    const range =
      direction === 'up'
        ? {
            from: Math.max(1, sourceLines[0].line - EXPAND_BY_LINES),
            to: sourceLines[0].line - 1,
          }
        : {
            from: sourceLines[sourceLines.length - 1].line + 1,
            // Add 1 to check for end-of-file:
            to: sourceLines[sourceLines.length - 1].line + EXPAND_BY_LINES + 1,
          };

    return getSources({
      key: hotspot.component.key,
      ...range,
      ...getBranchLikeQuery(branchLike),
    }).then((additionalLines) => {
      const { lastLine: previousLastLine } = this.state;

      const lastLine =
        direction === 'down' ? this.checkLastLine(additionalLines, range.to) : previousLastLine;

      let concatSourceLines;
      if (direction === 'up') {
        concatSourceLines = additionalLines.concat(sourceLines);
      } else {
        // remove extra sourceline if we didn't reach the end:
        concatSourceLines = sourceLines.concat(
          lastLine ? additionalLines : additionalLines.slice(0, -1),
        );
      }

      this.setState({
        lastLine,
        sourceLines: concatSourceLines,
      });
    });
  };

  handleSymbolClick = (highlightedSymbols: string[]) => {
    this.setState({ highlightedSymbols });
  };

  render() {
    const { branchLike, component, hotspot, selectedHotspotLocation } = this.props;
    const { highlightedSymbols, lastLine, loading, sourceLines, secondaryLocations } = this.state;

    const locations = locationsByLine([hotspot]);

    const sourceViewerFile = constructSourceViewerFile(hotspot, lastLine);

    return (
      <HotspotSnippetContainerRenderer
        component={component}
        branchLike={branchLike}
        highlightedSymbols={highlightedSymbols}
        hotspot={hotspot}
        loading={loading}
        locations={locations}
        onExpandBlock={this.handleExpansion}
        onSymbolClick={this.handleSymbolClick}
        onLocationSelect={this.props.onLocationSelect}
        sourceLines={sourceLines}
        sourceViewerFile={sourceViewerFile}
        secondaryLocations={secondaryLocations}
        selectedHotspotLocation={selectedHotspotLocation}
      />
    );
  }
}
