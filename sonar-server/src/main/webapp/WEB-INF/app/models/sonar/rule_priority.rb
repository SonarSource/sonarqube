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

require 'java'
include_class "org.sonar.api.rules.RulePriority"

class Sonar::RulePriority

  PRIORITY_INFO = Java::OrgSonarApiRules::RulePriority::INFO.ordinal()
  PRIORITY_MINOR = Java::OrgSonarApiRules::RulePriority::MINOR.ordinal()
  PRIORITY_MAJOR = Java::OrgSonarApiRules::RulePriority::MAJOR.ordinal()
  PRIORITY_CRITICAL = Java::OrgSonarApiRules::RulePriority::CRITICAL.ordinal()
  PRIORITY_BLOCKER = Java::OrgSonarApiRules::RulePriority::BLOCKER.ordinal()

  def self.to_s(failure_level)
    case failure_level
      when Java::OrgSonarApiRules::RulePriority::BLOCKER.ordinal()
        Java::OrgSonarApiRules::RulePriority::BLOCKER.to_s
      when Java::OrgSonarApiRules::RulePriority::CRITICAL.ordinal()
        Java::OrgSonarApiRules::RulePriority::CRITICAL.to_s
      when Java::OrgSonarApiRules::RulePriority::MAJOR.ordinal()
        Java::OrgSonarApiRules::RulePriority::MAJOR.to_s
      when Java::OrgSonarApiRules::RulePriority::MINOR.ordinal()
        Java::OrgSonarApiRules::RulePriority::MINOR.to_s
      when Java::OrgSonarApiRules::RulePriority::INFO.ordinal()
        Java::OrgSonarApiRules::RulePriority::INFO.to_s
    end
  end
  
  def self.id(priority)
    Java::OrgSonarApiRules::RulePriority.valueOf(priority).ordinal
  end
  
  def self.as_array
    @@priorities_a ||= []
    return @@priorities_a if @@priorities_a.size > 0
    @@priorities_a << PRIORITY_INFO
    @@priorities_a << PRIORITY_MINOR
    @@priorities_a << PRIORITY_MAJOR
    @@priorities_a << PRIORITY_CRITICAL
    @@priorities_a << PRIORITY_BLOCKER
  end
  
  def self.info?(priority)
    priority==PRIORITY_INFO
  end

  def self.minor?(priority)
    priority==PRIORITY_MINOR
  end
  
  def self.major?(priority)
    priority==PRIORITY_MAJOR
  end
  
  def self.critical?(priority)
    priority==PRIORITY_CRITICAL
  end
  
  def self.blocker?(priority)
    priority==PRIORITY_BLOCKER
  end
  
  def self.as_options
    @@priority_options ||= []
    return @@priority_options if @@priority_options.size > 0
    @@priority_options << ['Info', to_s(PRIORITY_INFO)]
    @@priority_options << ['Minor', to_s(PRIORITY_MINOR)]
    @@priority_options << ['Major', to_s(PRIORITY_MAJOR)]
    @@priority_options << ['Critical', to_s(PRIORITY_CRITICAL)]
    @@priority_options << ['Blocker', to_s(PRIORITY_BLOCKER)]
    @@priority_options    
  end
  
end
