import { expect } from 'chai';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import { IssuesTags } from '../../../../src/main/js/apps/overview/components/issues-tags';
import { WordCloud } from '../../../../src/main/js/components/charts/word-cloud';


const COMPONENT = { key: 'component-key' };

const TAGS = [
  { val: 'first', count: 3 },
  { val: 'second', count: 7000 },
  { val: 'third', count: 2 }
];


describe('IssuesTags', function () {
  it('should pass right data', function () {
    let renderer = TestUtils.createRenderer();
    renderer.render(<IssuesTags tags={TAGS} component={COMPONENT}/>);
    let output = renderer.getRenderOutput();
    expect(output.type).to.equal(WordCloud);
    expect(output.props.items).to.deep.equal([
      {
        "link": '/component_issues?id=component-key#resolved=false|tags=first',
        "size": 3,
        "text": 'first',
        "tooltip": 'Issues: 3'
      },
      {
        "link": '/component_issues?id=component-key#resolved=false|tags=second',
        "size": 7000,
        "text": 'second',
        "tooltip": 'Issues: 7k'
      },
      {
        "link": '/component_issues?id=component-key#resolved=false|tags=third',
        "size": 2,
        "text": 'third',
        "tooltip": 'Issues: 2'
      }
    ]);
  });
});
