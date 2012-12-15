require 'rake/testtask'

desc "Alias for spec:ci"
task :spec => "spec:ci"

desc "Alias for test:short"
task :test => "test:short"

desc "Alias for test:short19"
task :test19 => "test:short19"

desc "Alias for test:short18"
task :test18 => "test:short18"

namespace :test do
  desc "Compile test code"
  task :compile do
    ant "-q compile-test"
  end

  short_tests_18 = ['jruby', 'mri', 'rubicon']
  short_tests_19 = short_tests_18.map {|test| test + "19"}
  short_tests = short_tests_18 + short_tests_19
  long_tests_18 = short_tests_18 + ['spec:ji', 'spec:compiler', 'spec:ffi', 'spec:regression']
  long_tests_19 = short_tests_18.map {|test| test + "19"}
  slow_tests = ['test:slow', 'test:objectspace', 'test:tracing']
  long_tests = long_tests_18 + long_tests_19 + slow_tests
  all_tests_18 = long_tests_18.map {|test| test + ':all'}
  all_tests_19 = long_tests_19.map {|test| test + ':all'}
  all_tests = long_tests.map {|test| test + ":all"}

  desc "Run the short suite: #{short_tests.inspect}"
  task :short => [:compile, *short_tests]

  desc "Run the short 1.9 suite: #{short_tests_19.inspect}"
  task :short19 => [:compile, *short_tests_19]

  desc "Run the short 1.8 suite: #{short_tests_18.inspect}"
  task :short18 => [:compile, *short_tests_18]

  desc "Run the long suite: #{long_tests.inspect}"
  task :long => [:compile, *long_tests]

  desc "Run the long 1.9 suite: #{long_tests_19.inspect}"
  task :long19 => [:compile, *long_tests_19]

  desc "Run the long 1.8 suite: #{long_tests_18.inspect}"
  task :long18 => [:compile, *long_tests_18]

  desc "Run the comprehensive suite: #{all_tests}"
  task :all => [:compile, *all_tests]

  desc "Run the comprehensive 1.9 suite: #{all_tests_19}"
  task :all19 => [:compile, *all_tests_19]

  desc "Run the comprehensive 1.8 suite: #{all_tests_18}"
  task :all18 => [:compile, *all_tests_18]

  task :rake_targets => long_tests

  desc "Run tracing tests"
  task :tracing do
    Rake::TestTask.new('test:tracing') do |t|
      t.pattern = 'test/tracing/test_*.rb'
      t.verbose = true
      t.ruby_opts << '--debug'
      t.ruby_opts << '--1.8'
    end
  end
  
  compile_flags = {
    :default => :int,
    :int => ["-X-C"],
    :jit => ["-Xjit.threshold=0"],
    :aot => ["-X+C"],
    :ir_int => ["-X-CIR"],
    :all => [:int, :jit, :aot]
  }
  
  permute_tests(:mri19, compile_flags) do |t|
    files = []
    File.open('test/mri.1.9.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    ENV['EXCLUDE_DIR'] = 'test/externals/ruby1.9/excludes'
    t.ruby_opts << '--1.9'
    t.ruby_opts << '-I test/externals/ruby1.9'
    t.ruby_opts << '-I test/externals/ruby1.9/ruby'
    t.ruby_opts << '-r minitest/excludes'
  end
  
  permute_tests(:mri, compile_flags) do |t|
    files = []
    File.open('test/mri.1.8.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    ENV['EXCLUDE_DIR'] = 'test/externals/ruby1.9/excludes'
    t.ruby_opts << '--1.8'
  end

  permute_tests(:jruby19, compile_flags, 'test:compile') do |t|
    files = []
    File.open('test/jruby.1.9.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    t.ruby_opts << '-J-cp build/classes/test'
    t.ruby_opts << '--1.9'
  end

  permute_tests(:jruby, compile_flags, 'test:compile') do |t|
    files = []
    File.open('test/jruby.1.8.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    t.ruby_opts << '-J-cp build/classes/test'
    t.ruby_opts << '--1.8'
  end

  permute_tests(:rubicon19, compile_flags) do |t|
    files = []
    File.open('test/rubicon.1.9.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    t.ruby_opts << '-J-cp build/classes/test'
    t.ruby_opts << '--1.9'
    t.ruby_opts << '-X+O'
  end

  permute_tests(:rubicon, compile_flags) do |t|
    files = []
    File.open('test/rubicon.1.8.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    t.ruby_opts << '-J-cp build/classes/test'
    t.ruby_opts << '--1.8'
    t.ruby_opts << '-X+O'
  end

  permute_tests(:slow, compile_flags) do |t|
    files = []
    File.open('test/slow.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    t.ruby_opts << '-J-cp build/classes/test'
    t.ruby_opts << '--1.8'
  end

  permute_tests(:objectspace, compile_flags) do |t|
    files = []
    File.open('test/objectspace.index') do |f|
      f.lines.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.verbose = true
    t.ruby_opts << '-J-cp build/classes/test'
    t.ruby_opts << '--1.8'
    t.ruby_opts << '-X+O'
  end
end
