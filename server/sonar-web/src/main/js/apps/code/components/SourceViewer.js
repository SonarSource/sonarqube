import React, { Component } from 'react';

import BaseSourceViewer from '../../../components/source-viewer/main';


export default class SourceViewer extends Component {
  componentDidMount () {
    this.renderSourceViewer();
  }

  componentDidUpdate () {
    this.renderSourceViewer();
  }

  componentWillUnmount() {
    this.destroySourceViewer();
  }

  renderSourceViewer () {
    this.sourceViewer = new BaseSourceViewer();
    this.sourceViewer.render().$el.appendTo(this.refs.container);
    this.sourceViewer.open(this.props.component.uuid);
  }

  destroySourceViewer () {
    this.sourceViewer.destroy();
  }

  render () {
    return <div ref="container"></div>;
  }
}
