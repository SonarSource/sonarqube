import $ from 'jquery';
import _ from 'underscore';
import Popup from 'components/common/popup';
import Workspace from 'components/workspace/main';
import '../templates';

export default Popup.extend({
  template: Templates['source-viewer-coverage-popup'],

  events: {
    'click a[data-id]': 'goToFile'
  },

  onRender: function () {
    Popup.prototype.onRender.apply(this, arguments);
    this.$('.bubble-popup-container').isolatedScroll();
  },

  goToFile: function (e) {
    e.stopPropagation();
    var id = $(e.currentTarget).data('id');
    var RealWorkspace = Workspace.openComponent ? Workspace : require('components/workspace/main');
    RealWorkspace.openComponent({ uuid: id });
  },

  serializeData: function () {
    var row = this.options.row || {},
        tests = _.groupBy(this.collection.toJSON(), 'fileId'),
        testFiles = _.map(tests, function (testSet) {
          var test = testSet[0];
          return {
            file: {
              id: test.fileId,
              longName: test.fileLongName
            },
            tests: testSet
          };
        });
    _.extend(row, {
      lineHits: row[this.options.tests + 'LineHits'],
      conditions: row[this.options.tests + 'Conditions'],
      coveredConditions: row[this.options.tests + 'CoveredConditions']
    });
    return {
      testFiles: testFiles,
      tests: this.options.tests,
      row: row
    };
  }
});

