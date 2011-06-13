# modify it under the terms of the GNU Lesser General Public
 # License as published by the Free Software Foundation; either
 # version 3 of the License, or (at your option) any later version.
 #
 # Sonar is distributed in the hope that it will be useful,
 # but WITHOUT ANY WARRANTY; without even the implied warranty of
 # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 # Lesser General Public License for more details.
 #
 # You should have received a copy of the GNU Lesser General Public
 # License along with Sonar; if not, write to the Free Software
 # Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 #
module I18nHelper
  
  def self.java_locale()
    locale_parts = I18n.locale.to_s.split('-')
    return java.util.Locale.new(locale_parts[0]) if locale_parts.size == 1  
    return java.util.Locale.new(locale_parts[0], locale_parts[1]) if locale_parts.size >= 2  
  end

  def self.i18n 
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getI18n()
  end
  
  def self.translation(key, defaultText, *objects)
    i18n.translation(java_locale(), key, defaultText, objects.to_java)
  end
  
  def self.trans_widget(widget, key, default_text, *objects)
    i18n.translation(java_locale(), "view." + widget.widget_key + "." + key, default_text, objects.to_java)
  end

  def self.trans_page(view_id, key, default_text, *objects)
    i18n.translation(java_locale(), "view." + view_id + "." + key, default_text, objects.to_java)
  end

  def self.trans_tab(view_id, key, default_text, *objects)
    i18n.translation(java_locale(), "view." + view_id + "." + key, default_text, objects.to_java)
  end
  
  def self.trans_column(column_key, default_text, *objects)
    i18n.translation(java_locale(), "general_columns." + column_key, default_text, objects.to_java)
  end
  
  def self.trans_app_view(path, key, default_text, *objects)
    i18n.translation(java_locale(), "app.view." + path + "." + key, default_text, objects.to_java)
  end
  
  def self.trans_app_helper(path, key, default_text, *objects)
    i18n.translation(java_locale(), "app.helper." + path + "." + key, default_text, objects.to_java)
  end
  
  def self.trans_app_controller(path, key, default_text, *objects)
    i18n.translation(java_locale(), "app.controller." + path + "." + key, default_text, objects.to_java)
  end
  
  def self.trans_app_model(path, key, default_text, *objects)
    i18n.translation(java_locale(), "app.model." + path + "." + key, default_text, objects.to_java)
  end  
end