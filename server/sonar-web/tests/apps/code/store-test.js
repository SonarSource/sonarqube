import { expect } from 'chai';

import { current, bucket, initialState } from '../../../src/main/js/apps/code/reducers';
import {
    initComponentAction,
    browseAction,
    searchAction,
    updateQueryAction,
    startFetching,
    stopFetching
} from '../../../src/main/js/apps/code/actions';


const exampleComponent = { key: 'A' };
const exampleComponents = [
  { key: 'B' },
  { key: 'C' }
];


describe('Code :: Store', () => {
  //describe('action creators');

  describe('reducers', () => {
    describe('current', () => {
      describe('fetching', () => {
        it('should be set to true', () => {
          expect(current({ ...initialState, fetching: false }, startFetching()).fetching)
              .to.equal(true);
        });

        it('should be false', () => {
          expect(current({ ...initialState, fetching: true }, stopFetching()).fetching)
              .to.equal(false);
        });
      });
      describe('baseComponent', () => {
        it('should be set', () => {
          const component = {};
          expect(current(initialState, browseAction(component)).baseComponent)
              .to.equal(component);
        });

        it('should not be set for components with source code', () => {
          const file = { qualifier: 'FIL' };
          expect(current(initialState, browseAction(file, exampleComponents)).baseComponent)
              .to.be.null;
          const test = { qualifier: 'UTS' };
          expect(current(initialState, browseAction(test, exampleComponents)).baseComponent)
              .to.be.null;
        });
      });
      describe('components', () => {
        it('should be set', () => {
          const component = {};
          expect(current(initialState, browseAction(component, exampleComponents)).components)
              .to.deep.equal(exampleComponents);
        });

        it('should sort components by name', () => {
          const component = {};
          const componentsBefore = [
            { key: 'A', name: 'B' },
            { key: 'B', name: 'A' }
          ];
          const componentsAfter = [
            { key: 'B', name: 'A' },
            { key: 'A', name: 'B' }
          ];
          expect(current(initialState, browseAction(component, componentsBefore)).components)
              .to.deep.equal(componentsAfter);
        });

        it('should not be set for components with source code', () => {
          const file = { qualifier: 'FIL' };
          expect(current(initialState, browseAction(file, exampleComponents)).components)
              .to.be.null;
          const test = { qualifier: 'UTS' };
          expect(current(initialState, browseAction(test, exampleComponents)).components)
              .to.be.null;
        });
      });
      describe('breadcrumbs', () => {
        it('should be set', () => {
          expect(current(initialState, browseAction(exampleComponent, [], exampleComponents)).breadcrumbs)
              .to.deep.equal(exampleComponents);
        });

        it('should respect baseBreadcrumbs', () => {
          const baseBreadcrumbs = [{ key: 'BASE1' }];
          const breadcrumbsBefore = [{ key: 'BASE1' }, { key: 'BASE2' }, { key: 'C' }];
          const breadcrumbsAfter = [{ key: 'BASE2' }, { key: 'C' }];
          expect(current(
              { ...initialState, baseBreadcrumbs },
              browseAction(exampleComponent, [], breadcrumbsBefore)).breadcrumbs
          ).to.deep.equal(breadcrumbsAfter);
        });
      });
      describe('sourceViewer', () => {
        it('should be set for components with source code', () => {
          const file = { qualifier: 'FIL' };
          expect(current(initialState, browseAction(file, exampleComponents)).sourceViewer)
              .to.equal(file);
          const test = { qualifier: 'UTS' };
          expect(current(initialState, browseAction(test, exampleComponents)).sourceViewer)
              .to.equal(test);
        });

        it('should not be set for components without source code', () => {
          const project = { qualifier: 'TRK' };
          expect(current(initialState, browseAction(project, exampleComponents)).sourceViewer)
              .to.be.null;
          const unknown = {};
          expect(current(initialState, browseAction(unknown, exampleComponents)).sourceViewer)
              .to.be.null;
        });
      });
      describe('coverageMetric', () => {
        it('should be set to "coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: [
              { key: 'coverage', val: 13 }
            ]
          };

          expect(current(initialState, initComponentAction(componentWithCoverage)).coverageMetric)
              .to.equal('coverage');
        });

        it('should be set to "it_coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: [
              { key: 'it_coverage', val: 13 }
            ]
          };

          expect(current(initialState, initComponentAction(componentWithCoverage)).coverageMetric)
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

          expect(current(initialState, initComponentAction(componentWithCoverage)).coverageMetric)
              .to.equal('overall_coverage');
        });

        it('should fallback to "it_coverage"', () => {
          const componentWithCoverage = {
            ...exampleComponent,
            msr: []
          };

          expect(current(initialState, initComponentAction(componentWithCoverage)).coverageMetric)
              .to.equal('it_coverage');
        });
      });
      describe('baseBreadcrumbs', () => {
        it('should be empty', () => {
          const component = { key: 'A' };
          const breadcrumbs = [{ key: 'A' }];

          expect(current(initialState, initComponentAction(component, breadcrumbs)).baseBreadcrumbs)
              .to.have.length(0);
        });

        it('should set baseBreadcrumbs', () => {
          const component = { key: 'A' };
          const breadcrumbs = [{ key: 'BASE' }, { key: 'A' }];

          expect(current(initialState, initComponentAction(component, breadcrumbs)).baseBreadcrumbs)
              .to.have.length(1);
        });
      });
      describe('searchResults', () => {
        it('should be set', () => {
          const results = [{ key: 'A' }, { key: 'B' }];
          expect(current(initialState, searchAction(results)).searchResults)
              .to.deep.equal(results)
        });

        it('should be reset', () => {
          const results = [{ key: 'A' }, { key: 'B' }];
          const stateBefore = Object.assign({}, initialState, { searchResults: results });
          expect(current(stateBefore, browseAction(exampleComponent)).searchResults)
              .to.be.null;
        });
      });
      describe('searchQuery', () => {
        it('should be set', () => {
          expect(current(initialState, updateQueryAction('query')).searchQuery)
              .to.equal('query');
        });

        it('should be reset', () => {
          const stateBefore = Object.assign({}, initialState, { searchQuery: 'query' });
          expect(current(stateBefore, browseAction(exampleComponent)).searchQuery)
              .to.equal('');
        });
      });
    });
    describe('bucket', () => {
      it('should add initial component', () => {
        expect(bucket([], initComponentAction(exampleComponent)))
            .to.deep.equal([exampleComponent]);
      });

      it('should add browsed component', () => {
        const componentBefore = { key: 'A' };
        const childrenBefore = [{ key: 'B' }];
        const breadcrumbsBefore = [{ key: 'A' }];

        const bucketAfter = [
          { key: 'A', breadcrumbs: [{ key: 'A' }], children: [{ key: 'B' }] },
          { key: 'B', breadcrumbs: [{ key: 'A' }, { key: 'B' }] }
        ];

        expect(bucket([], browseAction(componentBefore, childrenBefore, breadcrumbsBefore)))
            .to.deep.equal(bucketAfter);
      });

      it('should merge new components', () => {
        const componentBefore = { key: 'A' };
        const childrenBefore = [{ key: 'B' }];
        const breadcrumbsBefore = [{ key: 'A' }];

        const bucketBefore = [
          { key: 'A' },
          { key: 'B' }
        ];

        const bucketAfter = [
          {
            key: 'A',
            breadcrumbs: [{ key: 'A' }],
            children: [{ key: 'B' }]
          },
          {
            key: 'B',
            breadcrumbs: [{ key: 'A' }, { key: 'B' }]
          }
        ];

        expect(bucket(bucketBefore, browseAction(componentBefore, childrenBefore, breadcrumbsBefore)))
            .to.deep.equal(bucketAfter);
      });

      it('should work twice in a row', () => {
        const componentA = { key: 'A' };
        const childrenA = [{ key: 'B' }];
        const breadcrumbsA = [{ key: 'A' }];

        const componentB = { key: 'B' };
        const childrenB = [{ key: 'C' }];
        const breadcrumbsB = [{ key: 'A' }, { key: 'B' }];

        const bucketAfter = [
          {
            key: 'A',
            breadcrumbs: [{ key: 'A' }],
            children: [{ key: 'B' }]
          },
          {
            key: 'B',
            breadcrumbs: [{ key: 'A' }, { key: 'B' }],
            children: [{ key: 'C' }]
          },
          {
            key: 'C',
            breadcrumbs: [{ key: 'A' }, { key: 'B' }, { key: 'C' }]
          }
        ];

        const afterFirstPass = bucket([], browseAction(componentA, childrenA, breadcrumbsA));
        const afterSecondPass = bucket(afterFirstPass, browseAction(componentB, childrenB, breadcrumbsB));

        expect(afterSecondPass)
            .to.deep.equal(bucketAfter);
      });
    });
  });
});
