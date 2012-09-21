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
  validates_presence_of :prop_key

  named_scope :with_key, lambda { |value| {:conditions => {:prop_key, value}} }
  named_scope :with_resource, lambda { |value| {:conditions => {:resource_id => value}} }
  named_scope :with_user, lambda { |value| {:conditions => {:user_id => value}} }

  def key
    prop_key
  end

  def value
    text_value
  end

  def self.hash(resource_id=nil, user_id=nil)
    properties = Property.with_resource(resource_id).with_user(user_id)

    Hash[properties.map { |prop| [prop.key, prop.value] }]
  end

  def self.clear(key, resource_id=nil, user_id=nil)
    prop = by_key(key, resource_id, user_id)
    if prop
      all(key, resource_id, user_id).delete_all
      Java::OrgSonarServerUi::JRubyFacade.getInstance().setGlobalProperty(key, nil) unless resource_id
    end
  end

  def self.by_key(key, resource_id=nil, user_id=nil)
    all(key, resource_id, user_id).first
  end

  def self.by_key_prefix(prefix)
    Property.find(:all, :conditions => ['prop_key like ?', prefix + '%'])
  end

  def self.value(key, resource_id=nil, default_value=nil, user_id=nil)
    property = by_key(key, resource_id, user_id)
    return default_value unless property

    property.text_value || default_value
  end

  def self.values(key, resource_id=nil, user_id=nil)
    value = value(key, resource_id, '', user_id)
    values = value.split(',')
    values.empty? ? [nil] : values.map { |v| v.gsub('%2C', ',') }
  end

  def self.set(key, value, resource_id=nil, user_id=nil)
    definition = Java::OrgSonarServerUi::JRubyFacade.getInstance().propertyDefinitions.get(key)
    if definition && definition.multi_values
      if value.kind_of? Array
        value = value.map { |v| v.gsub(',', '%2C') }.join(',')
      end
    elsif value.kind_of? Array
      value = value.first
    end

    text_value = (value.blank? ? nil : value.to_s)

    prop = by_key(key, resource_id, user_id)
    if prop
      if prop.text_value != text_value
        prop.text_value = text_value
        if prop.save
          Java::OrgSonarServerUi::JRubyFacade.getInstance().setGlobalProperty(key, text_value) unless resource_id
        end
      end
    else
      prop = Property.new(:prop_key => key, :text_value => text_value, :resource_id => resource_id, :user_id => user_id)
      if prop.save
        Java::OrgSonarServerUi::JRubyFacade.getInstance().setGlobalProperty(key, text_value) unless resource_id
      end
    end

    prop
  end

  def self.update(key, value, resource_id=nil, user_id=nil)
    set(key, value, resource_id, user_id)
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

  def java_definition
    @java_definition ||=
      begin
        Java::OrgSonarServerUi::JRubyFacade.getInstance().getPropertyDefinitions().get(key)
      end
  end

  def validation_error_message
    msg=''
    errors.each_full do |error|
      msg += Api::Utils.message("property.error.#{error}")
    end
    msg
  end

  private

  def self.all(key, resource_id=nil, user_id=nil)
    Property.with_key(key).with_resource(resource_id).with_user(user_id)
  end

  def validate
    if java_definition
      validation_result=java_definition.validate(text_value)
      errors.add_to_base(validation_result.getErrorKey()) unless validation_result.isValid()
    end
  end
end
