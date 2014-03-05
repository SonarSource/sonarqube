$ = jQuery

describe 'translation "t" suite', ->

  beforeEach ->
    window.messages =
      'something': 'SOMETHING'
      'something_with_underscore': 'SOMETHING_WITH_UNDERSCORE'
      'something_with{braces}': 'SOMETHING_WITH{braces}'

    window.SS =
      phrases:
        'something': 'SOMETHING ANOTHER'


  afterEach ->
    window.messages = window.SS = undefined


  it 'translates', ->
    expect(t('something')).toBe 'SOMETHING'


  it 'translates with underscore', ->
    expect(t('something_with_underscore')).toBe 'SOMETHING_WITH_UNDERSCORE'


  it 'translates with braces', ->
    expect(t('something_with{braces}')).toBe 'SOMETHING_WITH{braces}'


  it 'fallbacks to "translate"', ->
    window.messages = undefined
    expect(t('something')).toBe 'SOMETHING ANOTHER'


  it 'returns the key when no translation', ->
    expect(t('something_another')).toBe 'something_another'



describe 'translation "translate" suite', ->

  beforeEach ->
    window.SS =
      phrases:
        'something': 'SOMETHING'
        'something_with_underscore': 'SOMETHING_WITH_UNDERSCORE'
        'something_with{braces}': 'SOMETHING_WITH{braces}'


  afterEach ->
    window.messages = window.SS = undefined


  it 'translates', ->
    expect(translate('something')).toBe 'SOMETHING'


  it 'translates with underscore', ->
    expect(translate('something_with_underscore')).toBe 'SOMETHING_WITH_UNDERSCORE'


  it 'translates with braces', ->
    expect(translate('something_with{braces}')).toBe 'SOMETHING_WITH{braces}'


  it 'returns the key when no translation', ->
    expect(translate('something_another')).toBe 'something_another'

