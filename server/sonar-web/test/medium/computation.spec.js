define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Computation Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/ce/queue', 'computation-spec/queue.json')
          .mockFromFile('/api/ce/activity', 'computation-spec/history.json')
          .startApp('computation', { urlRoot: '/test/medium/base.html' })
          .checkElementCount('#computation-list li[data-id]', 1)
          .checkElementInclude('#computation-list', 'SonarSource :: Rule API')
          .checkElementInclude('#computation-list-footer', '1')
          .checkElementExist('.js-queue.selected')
          .clickElement('.js-history')
          .checkElementCount('#computation-list li[data-id]', 2)
          .checkElementInclude('#computation-list', 'Duration')
          .checkElementExist('.js-history.selected')
          .clickElement('.js-queue')
          .checkElementCount('#computation-list li[data-id]', 1);
    });

    bdd.it('should show more', function () {
      return this.remote
          .open('#past')
          .mockFromFile('/api/ce/queue', 'computation-spec/queue.json')
          .mockFromFile('/api/ce/activity', 'computation-spec/history-big-1.json')
          .startApp('computation', { urlRoot: '/test/medium/base.html' })
          .checkElementCount('#computation-list li[data-id]', 2)
          .clearMocks()
          .mockFromFile('/api/ce/activity', 'computation-spec/history-big-2.json', { data: { p: 2 } })
          .clickElement('#computation-fetch-more')
          .checkElementCount('#computation-list li[data-id]', 3);
    });
  });
});
