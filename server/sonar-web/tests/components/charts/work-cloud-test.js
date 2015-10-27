import React from 'react/addons';
import { expect } from 'chai';

import { WordCloud } from '../../../src/main/js/components/charts/word-cloud';


let TestUtils = React.addons.TestUtils;


describe('Word Cloud', function () {

  it('should display', function () {
    const items = [
      { size: 10, link: '#', text: 'SonarQube :: Server' },
      { size: 30, link: '#', text: 'SonarQube :: Web' },
      { size: 20, link: '#', text: 'SonarQube :: Search' }
    ];
    let chart = TestUtils.renderIntoDocument(<WordCloud items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithTag(chart, 'a')).to.have.length(3);
  });

});
