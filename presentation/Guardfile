require 'asciidoctor'
require 'erb'
require 'tilt'
require 'slim'

guard 'shell' do
  watch(%r{presentation.adoc$}) { |m|
    Asciidoctor.convert_file(m[0], :in_place => true, :template_dirs => ['asciidoctor-reveal.js/templates/slim'])
  }
end

guard 'livereload' do
  watch(%r{^.+\.(css|js|html)$})
end
