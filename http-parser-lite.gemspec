# -*- encoding: utf-8 -*-
# stub: http-parser-lite 1.0.1 ruby lib ext
# stub: ext/http-parser/extconf.rb

Gem::Specification.new do |s|
  s.name = "http-parser-lite".freeze
  s.version = "1.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze, "ext".freeze]
  s.authors = ["Bharanee Rathna".freeze]
  s.date = "2024-04-04"
  s.description = "A lite ruby wrapper around Joyent http-parser".freeze
  s.email = ["deepfryed@gmail.com".freeze]
  s.extensions = ["ext/http-parser/extconf.rb".freeze]
  s.files = ["CHANGELOG".freeze, "README.md".freeze, "ext/http-parser/extconf.rb".freeze, "ext/http-parser/http_parser.c".freeze, "ext/http-parser/http_parser.h".freeze, "ext/http-parser/ruby_http_parser.c".freeze, "lib/http-parser-lite.rb".freeze, "lib/http-parser.rb".freeze, "test/helper.rb".freeze, "test/test_http_parser.rb".freeze]
  s.homepage = "http://github.com/deepfryed/http-parser-lite".freeze
  s.rubygems_version = "3.4.19".freeze
  s.summary = "Simple wrapper around Joyent http-parser".freeze

  s.specification_version = 4

  s.add_development_dependency(%q<rake>.freeze, [">= 0"])
  s.add_development_dependency(%q<rake-compiler>.freeze, [">= 0"])
end
