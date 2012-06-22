# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "http-parser-lite"
  s.version = "0.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Bharanee Rathna"]
  s.date = "2012-06-23"
  s.description = "A lite ruby wrapper around Joyent http-parser"
  s.email = ["deepfryed@gmail.com"]
  s.extensions = ["ext/http-parser/extconf.rb"]
  s.files = ["ext/http-parser/http_parser.c", "ext/http-parser/ruby_http_parser.c", "ext/http-parser/http_parser.h", "ext/http-parser/extconf.rb", "test/helper.rb", "test/test_http_parser.rb", "lib/http-parser.rb", "README.md", "CHANGELOG"]
  s.homepage = "http://github.com/deepfryed/http-parser-lite"
  s.require_paths = ["lib", "ext"]
  s.rubygems_version = "1.8.24"
  s.summary = "Simple wrapper around Joyent http-parser"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
