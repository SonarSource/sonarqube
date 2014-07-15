$ = jQuery
window.baseUrl = ''
window.suppressTranslationWarnings = true


define [
  'component-viewer/main'
], (
  ComponentViewer
) ->

  describe 'Component Viewer Base Test Suite', ->

    beforeEach ->
      @viewer = new ComponentViewer()
      @viewer.render().$el.appendTo $('body')


    afterEach ->
      @viewer.close()


    it 'attaches to the page', ->
      expect($('.component-viewer', 'body').length).toBe 1


    it 'has all parts', ->
      expect(@viewer.$('.component-viewer-header').length).toBe 1
      expect(@viewer.$('.component-viewer-source').length).toBe 1
      expect(@viewer.$('.component-viewer-workspace').length).toBe 1
