require 'date'
require 'pathname'
require 'rake'
require 'rake/clean'
require 'rake/testtask'

$rootdir = Pathname.new(__FILE__).dirname
$gemspec = Gem::Specification.new do |s|
  s.name              = 'http-parser-lite'
  s.version           = '0.3.0'
  s.date              = Date.today    
  s.authors           = ['Bharanee Rathna']
  s.email             = ['deepfryed@gmail.com']
  s.summary           = 'Simple wrapper around Joyent http-parser'
  s.description       = 'A lite ruby wrapper around Joyent http-parser'
  s.homepage          = 'http://github.com/deepfryed/http-parser-lite'
  s.files             = Dir['ext/**/*.{c,h}'] + Dir['{ext,test,lib}/**/*.rb'] + %w(README.md CHANGELOG)
  s.extensions        = %w(ext/http-parser/extconf.rb)
  s.require_paths     = %w(lib ext)

  s.add_development_dependency('rake')
end

desc 'Generate gemspec'
task :gemspec do 
  $gemspec.date = Date.today
  File.open('http-parser-lite.gemspec', 'w') {|fh| fh.write($gemspec.to_ruby)}
end

desc 'compile extension'
task :compile do
  Dir.chdir('ext/http-parser') do
    system('ruby extconf.rb && make -j2') or raise 'unable to compile http-parser'
  end
end

Rake::TestTask.new(:test) do |test|
  test.libs   << 'ext' << 'lib' << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
end

task default: :test
task :test => [:compile]
