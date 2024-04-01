require 'date'
require 'pathname'
require 'rake'
require 'rake/clean'
require 'rake/testtask'
require 'rubygems/package_task'

$rootdir = Pathname.new(__FILE__).dirname
$gemspec = Gem::Specification.new do |s|
  s.name              = 'http-parser-lite'
  s.version           = '1.0.0'
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
  s.add_development_dependency('rake-compiler')
end

desc 'Generate gemspec'
task :gemspec do
  $gemspec.date = Date.today
  File.open('http-parser-lite.gemspec', 'w') {|fh| fh.write($gemspec.to_ruby)}
end

Gem::PackageTask.new($gemspec) do |pkg|
end

desc 'compile extension'
if RUBY_PLATFORM =~ /java/
  require 'rake/javaextensiontask'
  Rake::JavaExtensionTask.new('http-parser', $gemspec) do |ext|
    jruby_home  = RbConfig::CONFIG['prefix']
    ext.ext_dir = 'ext/http-parser'
    ext.lib_dir = 'lib'
    jars = ["#{jruby_home}/lib/jruby.jar"] + FileList['ext/vendor/*.jar']
    ext.classpath = jars.map {|x| File.expand_path x}.join ':'
  end
else
  task :compile do
    Dir.chdir('ext/http-parser') do
      system('ruby extconf.rb && make -j2') or raise 'unable to compile http-parser'
    end
  end
end

Rake::TestTask.new(:test) do |test|
  test.libs   << 'ext' << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
end

task default: :test
task :test => [:compile]

desc 'tag release and build gem'
task :release => [:test, :gemspec] do
  system("git tag -m 'version #{$gemspec.version}' v#{$gemspec.version}") or raise "failed to tag release"
  system("rake package")
  system("rake java package")
end
