 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2009 SonarSource SA
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
class RulesCategory < ActiveRecord::Base
  has_many :rule, :order => 'name'  

  @@categories = nil
  def self.all
    if @@categories.nil?
      @@categories = find(:all, :order => 'name')
    end
    @@categories
  end

  def self.by_id(key)
    all.each do |categ|
      return categ if categ.id==key
    end
    nil
  end
  
  def self.by_name(name)
    all.each do |categ|
      return categ if categ.name==name
    end
    nil    
  end
  
  def self.by_key(key)
    if key.to_i>0
      by_id(key.to_i)
    else
      by_name(key)
    end
  end

  def self.clear_cache
    @@categories=nil
  end

  def self.select_choices
    all.collect {|rc| [ rc.name, rc.id ] }.sort
  end
  
end
