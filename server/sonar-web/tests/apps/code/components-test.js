import chai, { expect } from 'chai';
import sinon from 'sinon';
import sinonChai from 'sinon-chai';
import { shallow } from 'enzyme';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import Breadcrumb from '../../../src/main/js/apps/code/components/Breadcrumb';
import Breadcrumbs from '../../../src/main/js/apps/code/components/Breadcrumbs';
import Component from '../../../src/main/js/apps/code/components/Component';
import ComponentDetach from '../../../src/main/js/apps/code/components/ComponentDetach';
import ComponentMeasure from '../../../src/main/js/apps/code/components/ComponentMeasure';
import ComponentName from '../../../src/main/js/apps/code/components/ComponentName';
import Components from '../../../src/main/js/apps/code/components/Components';
import ComponentsEmpty from '../../../src/main/js/apps/code/components/ComponentsEmpty';
import Truncated from '../../../src/main/js/apps/code/components/Truncated';

import { getComponentUrl } from '../../../src/main/js/helpers/urls';
import QualifierIcon from '../../../src/main/js/components/shared/qualifier-icon';


chai.use(sinonChai);


const measures = [
  { key: 'ncloc', val: 9757 }
];
const exampleComponent = {
  uuid: 'A1',
  key: 'A',
  name: 'AA',
  qualifier: 'TRK',
  msr: measures
};
const exampleComponent2 = { uuid: 'B2', key: 'B' };
const exampleComponent3 = { uuid: 'C3', key: 'C' };
const exampleOnBrowse = sinon.spy();


describe('Code :: Components', () => {

  describe('<Breadcrumb/>', () => {
    it('should render <ComponentName/>', () => {
      const output = shallow(
          <Breadcrumb
              component={exampleComponent}
              onBrowse={exampleOnBrowse}/>
      );

      expect(output.type())
          .to.equal(ComponentName);
      expect(output.props())
          .to.deep.equal({ component: exampleComponent, onBrowse: exampleOnBrowse })
    });
  });

  describe('<Breadcrumbs/>', () => {
    let output;
    let list;

    before(() => {
      output = shallow(
          <Breadcrumbs
              breadcrumbs={[exampleComponent, exampleComponent2, exampleComponent3]}
              onBrowse={exampleOnBrowse}/>);
      list = output.find(Breadcrumb);
    });

    it('should render list of <Breadcrumb/>s', () => {
      expect(list)
          .to.have.length(3);
      expect(list.at(0).prop('component'))
          .to.equal(exampleComponent);
      expect(list.at(1).prop('component'))
          .to.equal(exampleComponent2);
      expect(list.at(2).prop('component'))
          .to.equal(exampleComponent3);
    });

    it('should pass onBrowse to all components except the last one', () => {
      expect(list.at(0).prop('onBrowse'))
          .to.equal(exampleOnBrowse);
      expect(list.at(1).prop('onBrowse'))
          .to.equal(exampleOnBrowse);
      expect(list.at(2).prop('onBrowse'))
          .to.equal(null);
    });
  });

  describe('<Component/>', () => {
    let output;

    before(() => {
      output = shallow(
          <Component
              component={exampleComponent}
              coverageMetric="coverage"
              onBrowse={exampleOnBrowse}/>);
    });

    it('should render <ComponentName/>', () => {
      const findings = output.find(ComponentName);
      expect(findings)
          .to.have.length(1);
      expect(findings.first().props())
          .to.deep.equal({ component: exampleComponent, onBrowse: exampleOnBrowse });
    });

    it('should render <ComponentMeasure/>s', () => {
      const findings = output.find(ComponentMeasure);
      expect(findings)
          .to.have.length(5);
      expect(findings.at(0).props())
          .to.deep.equal({ component: exampleComponent, metricKey: 'ncloc', metricType: 'SHORT_INT' });
      expect(findings.at(1).props())
          .to.deep.equal({ component: exampleComponent, metricKey: 'sqale_index', metricType: 'SHORT_WORK_DUR' });
      expect(findings.at(2).props())
          .to.deep.equal({ component: exampleComponent, metricKey: 'violations', metricType: 'SHORT_INT' });
      expect(findings.at(3).props())
          .to.deep.equal({ component: exampleComponent, metricKey: 'coverage', metricType: 'PERCENT' });
      expect(findings.at(4).props())
          .to.deep.equal({ component: exampleComponent, metricKey: 'duplicated_lines_density', metricType: 'PERCENT' });
    });

    it('should render <ComponentDetach/>', () => {
      const findings = output.find(ComponentDetach);
      expect(findings)
          .to.have.length(1);
      expect(findings.first().props())
          .to.deep.equal({ component: exampleComponent });
    });
  });

  describe('<ComponentDetach/>', () => {
    it('should render link', () => {
      const output = shallow(
          <ComponentDetach component={exampleComponent}/>);
      const expectedUrl = getComponentUrl(exampleComponent.key);

      expect(output.type())
          .to.equal('a');
      expect(output.prop('target'))
          .to.equal('_blank');
      expect(output.prop('href'))
          .to.equal(expectedUrl);
    });
  });

  describe('<ComponentMeasure/>', () => {
    it('should render formatted measure', () => {
      const output = shallow(
          <ComponentMeasure
              component={exampleComponent}
              metricKey="ncloc"
              metricType="SHORT_INT"/>);

      expect(output.text())
          .to.equal('9.8k');
    });

    it('should not render measure', () => {
      const output = shallow(
          <ComponentMeasure
              component={exampleComponent}
              metricKey="random"
              metricType="SHORT_INT"/>);

      expect(output.text())
          .to.equal('');
    });
  });

  describe('<ComponentName/>', () => {
    it('should render <QualifierIcon/>', () => {
      const output = shallow(
          <ComponentName
              component={exampleComponent}
              onBrowse={exampleOnBrowse}/>);
      const findings = output.find(QualifierIcon);

      expect(findings)
          .to.have.length(1);
      expect(findings.first().prop('qualifier'))
          .to.equal('TRK');
    });

    it('should render link to component', () => {
      const output = shallow(
          <ComponentName
              component={exampleComponent}
              onBrowse={exampleOnBrowse}/>);
      const findings = output.find('a');

      expect(findings)
          .to.have.length(1);
      expect(findings.first().text())
          .to.equal('AA');
    });

    it('should not render link to component', () => {
      const output = shallow(
          <ComponentName
              component={exampleComponent}
              onBrowse={null}/>);
      const findings = output.find('span');

      expect(output.find('a'))
          .to.have.length(0);
      expect(findings)
          .to.have.length(1);
      expect(findings.first().text())
          .to.equal('AA');
    });

    it('should browse on click', () => {
      const spy = sinon.spy();
      const preventDefaultSpy = sinon.spy();
      const output = shallow(
          <ComponentName
              component={exampleComponent}
              onBrowse={spy}/>);
      const findings = output.find('a');

      findings.first().simulate('click', { preventDefault: preventDefaultSpy });

      expect(preventDefaultSpy).to.have.been.called;
      expect(spy).to.have.been.calledWith(exampleComponent);
    });
  });

  describe('<Components/>', () => {
    let output;

    before(() => {
      output = shallow(
          <Components
              baseComponent={exampleComponent}
              components={[exampleComponent2, exampleComponent3]}
              onBrowse={exampleOnBrowse}/>);
    });

    it('should render base component', () => {
      const findings = output.findWhere(node => {
        return node.type() === Component && node.prop('component') === exampleComponent;
      });

      expect(findings)
          .to.have.length(1);
      expect(findings.first().prop('onBrowse'))
          .to.not.be.ok;
    });

    it('should render children component', () => {
      const findings = output.findWhere(node => {
        return node.type() === Component && node.prop('component') !== exampleComponent;
      });

      expect(findings)
          .to.have.length(2);
      expect(findings.at(0).prop('onBrowse'))
          .to.equal(exampleOnBrowse)
    });
  });

  describe('<ComponentsEmpty/>', () => {
    it('should render', () => {
      const output = shallow(<ComponentsEmpty/>);

      expect(output.text())
          .to.include('no_results');
    });
  });

  describe('<Truncated/>', () => {
    it('should render and set title', () => {
      const output = shallow(<Truncated title="ABC">123</Truncated>);

      expect(output.type())
          .to.equal('span');
      expect(output.text())
          .to.equal('123');
      expect(output.prop('data-title'))
          .to.equal('ABC');
    });
  });
});
