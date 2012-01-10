#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
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
# License along with {library}; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class Property < ActiveRecord::Base

  def key
    prop_key
  end

  def value
    text_value
  end

  def self.hash(resource_id=nil)
    hash={}
    Property.find(:all, :conditions => {'resource_id' => resource_id, 'user_id' => nil}).each do |prop|
      hash[prop.key]=prop.value
    end
    hash
  end

  def self.value(key, resource_id=nil, default_value=nil)
    prop=Property.find(:first, :conditions => {'prop_key' => key, 'resource_id' => resource_id, 'user_id' => nil})
    if prop
      prop.text_value || default_value
    else
      default_value
    end
  end

  def self.values(key, resource_id=nil)
    Property.find(:all, :conditions => {'prop_key' => key, 'resource_id' => resource_id, 'user_id' => nil}).collect { |p| p.text_value }
  end

  def self.set(key, value, resource_id=nil)
    Property.delete_all('prop_key' => key, 'resource_id' => resource_id, 'user_id' => nil)
    Property.create(:prop_key => key, :text_value => value.to_s, :resource_id => resource_id)
    reload_java_configuration
  end

  def self.clear(key, resource_id=nil)
    Property.delete_all('prop_key' => key, 'resource_id' => resource_id, 'user_id' => nil)
    reload_java_configuration
  end

  def self.by_key(key, resource_id=nil)
    Property.find(:first, :conditions => {'prop_key' => key, 'resource_id' => resource_id, 'user_id' => nil})
  end

  def self.by_key_prefix(prefix)
    Property.find(:all, :conditions => ["prop_key like ?", prefix + '%'])
  end

  def self.update(key, value)
    property = Property.find(:first, :conditions => {:prop_key => key, :resource_id => nil, :user_id => nil});
    property.text_value = value
    property.save
    reload_java_configuration
    property
  end

  def to_hash_json
    {:key => key, :value => value.to_s}
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0))
    xml.property do
      xml.key(prop_key)
      xml.value { xml.cdata!(text_value.to_s) }
    end
    xml
  end

  private

  def self.reload_java_configuration
    Java::OrgSonarServerUi::JRubyFacade.getInstance().reloadConfiguration()
  end
end
