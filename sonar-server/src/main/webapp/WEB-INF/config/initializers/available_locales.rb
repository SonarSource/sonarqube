# Only for Rails 2.2. It is included in Rails 2.3.
# Get loaded locales conveniently 
# See http://rails-i18n.org/wiki/pages/i18n-available_locales
module I18n
  class << self  
    def available_locales; backend.available_locales; end  
  end  
  module Backend 
    class Simple 
      def available_locales; translations.keys.collect { |l| l.to_s }.sort; end  
    end  
  end 
end 

# You need to "force-initialize" loaded locales 
I18n.backend.send(:init_translations) 
AVAILABLE_LOCALES = I18n.backend.available_locales 