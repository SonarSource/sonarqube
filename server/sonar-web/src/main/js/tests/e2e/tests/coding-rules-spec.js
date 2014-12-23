var lib = require('../lib'),
    testName = lib.testName('Coding Rules');


lib.initMessages();
lib.changeWorkingDirectory('coding-rules-spec');


casper.test.begin(testName('Admin Tests'), function suite(test) {

  var showId = null;
  var updateId = null;

  casper.start(lib.buildUrl('coding-rules-old'), function() {
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_initial.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkInitialPageLoad() {

    casper.waitForSelector('.navigator-header', function checkAdminHeader() {
      test.assertExist('button#coding-rules-create-rule');
    });


    casper.waitForSelector('h3.coding-rules-detail-header', function showFirstRule() {
      test.assertSelectorHasText('.coding-rules-detail-description-extra', 'Xoo shall not pass');

      test.assertExists('button#coding-rules-detail-extend-description');
    });


    casper.then(function editNote() {
      casper.click('button#coding-rules-detail-extend-description');
      test.assertSelectorHasText('textarea#coding-rules-detail-extend-description-text', 'As per the [Book of Xoo](http://xoo.sonarsource.com/book):\n> Xoo shall not pass!');

      casper.sendKeys('textarea#coding-rules-detail-extend-description-text', 'Xoo must pass');

      updateId = lib.mockRequestFromFile('/api/rules/update', 'edit_note_x1.json');
      casper.click('button#coding-rules-detail-extend-description-submit');

      casper.wait(500, function showUpdatedRule() {
        test.assertSelectorHasText('.coding-rules-detail-description-extra', 'Xoo must pass');
      });

    });
  });

  casper.run(function() {
    test.done();
  });
});


casper.test.begin(testName('Activation Tests'), function suite(test) {

  var showId = null;
  var activateId = null;

  casper.start(lib.buildUrl('coding-rules-old#rule_key=squid-xoo:x1'), function() {
    lib.clearRequestMocks();
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_x1.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkRuleActivation() {

    casper.waitForSelector('div.coding-rules-detail-quality-profiles-section', function showRuleActivation() {
      test.assertExists('#coding-rules-detail-quality-profiles');

      test.assertElementCount('.coding-rules-detail-quality-profile', 1);

      test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'A nice');
      test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'true');
      test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', '0');
    });


    casper.then(function editActiveRule() {
      casper.click('button.coding-rules-detail-quality-profile-change');

      casper.waitForSelector('button#coding-rules-quality-profile-activation-activate', function checkActivationPopup() {
        test.assertElementCount('.modal-body .property', 5 /* quality profile, severity, 3 parameters */);

        test.assertSelectorHasText('.modal-body .property textarea[name="textParameter"]', 'A nice\ntext parameter\nwith newlines.');
        test.assertSelectorHasText('.modal-body .property input[name="skipLines"]', '');
        test.assertEval(function checkPlaceHolder() {
          return $j('.modal-body .property input[name="skipLines"]').attr('placeholder') === '0'
        });
        test.assertEval(function checkTrueIsSelected() {
          return $j('.modal-body .property select[name="acceptWhitespace"]').val() === 'true'
        });
      });
    });

    casper.then(function updateParameters() {
      casper.sendKeys('textarea[name="textParameter"]', '\nUpdated');
      casper.sendKeys('input[name="skipLines"]', '5');
      casper.evaluate(function selectDefault() {
          $j('select[name="acceptWhitespace"]').val('false').change();
      });

      lib.clearRequestMock(showId);
      showId = lib.mockRequestFromFile('/api/rules/show', 'update_parameters_x1.json');
      activateId = lib.mockRequest('/api/qualityprofiles/activate_rule', '');

      casper.click('button#coding-rules-quality-profile-activation-activate');

      casper.wait(500, function showUpdatedParameters() {
        test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'A nice');
        test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'false');
        test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', '5');
      });

    });

    casper.then(function deactivateRule() {
      casper.click('button.coding-rules-detail-quality-profile-deactivate');

      casper.waitForSelector('button[data-confirm="yes"]', function checkConfirmPopup() {
        lib.clearRequestMock(showId);
        showId = lib.mockRequestFromFile('/api/rules/show', 'deactivate_x1.json');
        lib.mockRequest('/api/qualityprofiles/deactivate_rule', '');

        casper.click('button[data-confirm="yes"]');
      });

      casper.wait(500, function showUpdatedParameters() {
        test.assertElementCount('.coding-rules-detail-quality-profile', 0);
      });
    });

    casper.then(function activateRule() {
      casper.click('button#coding-rules-quality-profile-activate');

      casper.waitForSelector('button#coding-rules-quality-profile-activation-activate', function checkActivationPopup() {

        lib.clearRequestMock(showId);
        showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
        lib.clearRequestMock(activateId);
        activateId = lib.mockRequest('/api/qualityprofiles/activate_rule', '');

        casper.click('button#coding-rules-quality-profile-activation-activate');

        casper.wait(500, function showUpdatedParameters() {
          test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'A nice');
          test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'true');
          test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', '0');
        });

      });

    });

  });


  casper.run(function() {
    test.done();
  });
});


casper.test.begin(testName('Tag Navigation Test'), function suite(test) {

  casper.start(lib.buildUrl('coding-rules-old#tags=polop,bug,pilip,unused,palap'), function() {
    lib.clearRequestMocks();
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_x1.json');
    lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkTagFilterRestored() {
    casper.waitForSelector('.navigator-filters', function checkDefaultFilters() {
      test.assertElementCount('.navigator-filter-disabled', 11 /* Tag is enabled */);
      test.assertSelectorHasText('.navigator-filter[data-property="tags"] .navigator-filter-value', 'bug, unused');
    });
  });

  casper.run(function() {
    test.done();
  });
});
