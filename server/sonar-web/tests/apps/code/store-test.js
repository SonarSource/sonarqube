import { expect } from 'chai';

import {
    fetching,
    baseComponent,
    components,
    breadcrumbs,
    sourceViewer,
    coverageMetric
} from '../../../src/main/js/apps/code/reducers';
import {
    requestComponents,
    receiveComponents,
    showSource
} from '../../../src/main/js/apps/code/actions';


const exampleComponent = { key: 'A' };


describe('Code :: Store', () => {
  //describe('action creators');

  describe('reducers', () => {
    describe('fetching', () => {
      it('should be initially false', () => {
        expect(fetching(undefined, {}))
            .to.equal(false);
      });

      it('should be true after requesting components', () => {
        expect(fetching(false, requestComponents()))
            .to.equal(true);
      });

      it('should be false after receiving components', () => {
        expect(fetching(true, receiveComponents({}, [])))
            .to.equal(false);
      });
    });

    describe('baseComponent', () => {
      it('should not be set after requesting components', () => {
        const component = {};
        expect(baseComponent(null, requestComponents(component)))
            .to.equal(null);
      });

      it('should be set after receiving components', () => {
        const component = {};
        expect(baseComponent(null, receiveComponents(component, [])))
            .to.equal(component);
      });
    });

    describe('components', () => {
      it('should be set after receiving components', () => {
        const list = [exampleComponent];
        expect(components(null, receiveComponents({}, list)))
            .to.equal(list);
      });
    });

    describe('breadcrumbs', () => {
      it('should push new component on BROWSE', () => {
        const stateBefore = [];
        const stateAfter = [exampleComponent];
        expect(breadcrumbs(stateBefore, requestComponents(exampleComponent)))
            .to.deep.equal(stateAfter);
      });

      it('should push new component on SHOW_SOURCE', () => {
        const stateBefore = [];
        const stateAfter = [exampleComponent];
        expect(breadcrumbs(stateBefore, showSource(exampleComponent)))
            .to.deep.equal(stateAfter);
      });

      it('should cut the tail', () => {
        const stateBefore = [{ key: 'B' }, exampleComponent, { key: 'C' }];
        const stateAfter = [{ key: 'B' }, exampleComponent];
        expect(breadcrumbs(stateBefore, requestComponents(exampleComponent)))
            .to.deep.equal(stateAfter);
      });
    });

    describe('sourceViewer', () => {
      it('should be set on SHOW_SOURCE', () => {
        expect(sourceViewer(null, showSource(exampleComponent)))
            .to.equal(exampleComponent);
      });

      it('should be unset on BROWSE', () => {
        expect(sourceViewer(exampleComponent, requestComponents({})))
            .to.equal(null);
      });

      describe('coverageMetric', () => {
        it('should be initially null', () => {
          expect(coverageMetric(undefined, {}))
              .to.equal(null);
        });

        it('should be set to "coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: [
              { key: 'coverage', val: 13 }
            ]
          };

          expect(coverageMetric(null, requestComponents(componentWithCoverage)))
              .to.equal('coverage');
        });

        it('should be set to "it_coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: [
              { key: 'it_coverage', val: 13 }
            ]
          };

          expect(coverageMetric(null, requestComponents(componentWithCoverage)))
              .to.equal('it_coverage');
        });

        it('should be set to "overall_coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: [
              { key: 'coverage', val: 11 },
              { key: 'it_coverage', val: 12 },
              { key: 'overall_coverage', val: 13 }
            ]
          };

          expect(coverageMetric(null, requestComponents(componentWithCoverage)))
              .to.equal('overall_coverage');
        });

        it('should fallback to "it_coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: []
          };

          expect(coverageMetric(null, requestComponents(componentWithCoverage)))
              .to.equal('it_coverage');
        });

        it('should not be reset after set once', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: [
              { key: 'coverage', val: 13 }
            ]
          };

          expect(coverageMetric('overall_coverage', requestComponents(componentWithCoverage)))
              .to.equal('overall_coverage');
        });
      });
    });
  });
});
