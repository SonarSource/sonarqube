import $ from 'jquery';
import _ from 'underscore';
import Popup from '../../common/popup';
import Workspace from '../../workspace/main';
import Template from '../templates/source-viewer-coverage-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click a[data-uuid]': 'goToFile'
  },

  onRender: function () {
    Popup.prototype.onRender.apply(this, arguments);
    this.$('.bubble-popup-container').isolatedScroll();
  },

  goToFile: function (e) {
    e.stopPropagation();
    var uuid = $(e.currentTarget).data('uuid');
    Workspace.openComponent({ uuid: uuid });
  },

  serializeData: function () {
    var row = this.options.row || {},
        tests = _.groupBy(this.collection.toJSON(), 'fileUuid'),
        testFiles = _.map(tests, function (testSet) {
          var test = testSet[0];
          return {
            file: {
              uuid: test.fileUuid,
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

