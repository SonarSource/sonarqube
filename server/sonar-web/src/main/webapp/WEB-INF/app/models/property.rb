#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
class Property < ActiveRecord::Base
  validates_presence_of :prop_key

  named_scope :with_keys, lambda { |values| {:conditions => ['prop_key in (?)', values]} }
  named_scope :with_key, lambda { |value| {:conditions => {:prop_key, value}} }
  named_scope :with_key_prefix, lambda { |value| {:conditions => ['prop_key like ?', value + '%']} }
  named_scope :with_value, lambda { |value| {:conditions => ['text_value like ?', value]} }
  named_scope :with_resource, lambda { |value| {:conditions => {:resource_id => value}} }
  named_scope :with_user, lambda { |value| {:conditions => {:user_id => value}} }
  named_scope :with_resources, :conditions => 'resource_id is not null'
  named_scope :with_users, :conditions => 'user_id is not null'

  EXISTING_PASSWORD = '{{*******************}}'

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
      setGlobalProperty(key, nil, resource_id, user_id)
    end
    prop
  end

  def self.clear_for_resources(key, value=nil)
    scope = Property.with_resources().with_key(key)
    if value
      scope = scope.with_value(value)
    end
    scope.delete_all
  end

  def self.clear_for_users(key)
    Property.with_users().with_key(key).delete_all
  end

  def self.by_key(key, resource_id=nil, user_id=nil)
    all(key, resource_id, user_id).first
  end

  def self.by_key_prefix(prefix)
    Property.with_key_prefix(prefix)
  end

  def self.value(key, resource_id=nil, default_value=nil, user_id=nil)
    property = by_key(key, resource_id, user_id)
    return default_value unless property

    property.text_value || default_value
  end

  def self.values(key, resource_id=nil, user_id=nil)
    value = value(key, resource_id, '', user_id)
    values = Property.string_to_array_value(value)
    values.empty? ? [nil] : values
  end

  def self.set(key, value, resource_id=nil, user_id=nil)
    if value.kind_of? Array
      value = drop_trailing_blank_values(value)
      value = Property.new({:prop_key => key}).multi_values? ? array_value_to_string(value) : value.first
    end

    text_value = value.to_s if defined? value
    text_value = nil if text_value.blank?

    # Load Java property definition
    property_def = field_property_def(key) || property_def(key)

    if text_value.blank?
      return Property.clear(key, resource_id)
    end

    prop = by_key(key, resource_id, user_id)
    if prop && prop.text_value == text_value
      return prop
    end

    if !prop
      prop = Property.new(:prop_key => key, :resource_id => resource_id, :user_id => user_id)
      # Do not update password that wasn't updated
    elsif property_def.type().to_s == PropertyType::TYPE_PASSWORD && text_value == EXISTING_PASSWORD
      text_value = prop.text_value
    end

    prop.text_value = text_value
    if prop.save
      setGlobalProperty(key, text_value, resource_id, user_id)
    end

    prop
  end

  def self.update(key, value, resource_id=nil, user_id=nil)
    set(key, value, resource_id, user_id)
  end

  def to_hash_json
    hash = {:key => key, :value => value.to_s}
    hash.merge!(:values => Property.string_to_array_value(value.to_s)) if multi_values?
    hash
  end

  def multi_values?
    java_definition && (java_definition.multi_values? || !java_definition.fields.blank?)
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0))
    xml.property do
      xml.key(prop_key)
      xml.value { xml.cdata!(value.to_s) }
      Property.string_to_array_value(value.to_s).each { |v| xml.values { xml.cdata!(v) } } if multi_values?
    end
    xml
  end

  def java_definition
    @java_definition ||= Property.property_def(key)
  end

  def java_field_definition
    @java_field_definition ||= Property.field_property_def(key)
  end

  def validation_error_message
    msg=''
    errors.each_full do |error|
      msg += Api::Utils.message("property.error.#{error}")
    end
    msg
  end

  def self.string_to_array_value(value)
    value.split(',').map { |v| v.gsub('%2C', ',') }
  end

  def self.array_value_to_string(array)
    array.map { |v| v.gsub(',', '%2C') }.join(',')
  end

  def self.setGlobalProperty(key, value, resource_id, user_id)
    Api::Utils.java_facade.setGlobalProperty(key, value) unless (resource_id || user_id)
  end

  private

  def self.all(key, resource_id=nil, user_id=nil)
    Property.with_key(key).with_resource(resource_id).with_user(user_id)
  end

  def self.drop_trailing_blank_values(array)
    array.reverse.drop_while(&:blank?).reverse
  end

  def validate
    validate_property()
    validate_field()
  end

  def validate_property
    if java_definition
      validation_result = java_definition.validate(text_value)
      errors.add_to_base(validation_result.errorKey) unless validation_result.isValid()
    end
  end

  def validate_field
    if java_field_definition
      validation_result = java_field_definition.validate(text_value)
      errors.add_to_base(validation_result.errorKey) unless validation_result.isValid()
    end
  end

  def self.property_def(key)
    begin
      Api::Utils.java_facade.propertyDefinitions.get(key)
    end
  end

  def self.field_property_def(key)
    begin
      if /(.*)\..*\.(.*)/.match(key)
        property_definition = Api::Utils.java_facade.propertyDefinitions.get(Regexp.last_match(1))
        if property_definition
          property_definition.fields.find { |field| field.key == Regexp.last_match(2) }
        end
      end
    end
  end

end
