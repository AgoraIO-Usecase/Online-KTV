Pod::Spec.new do |spec|
  spec.name         = "AgoraLrcScore"
  spec.version      = "1.0.0"
  spec.summary      = "AgoraLrcScore"
  spec.description  = "AgoraLrcScore"

  spec.homepage     = "https://www.agora.io"
  spec.license      = "MIT"
  spec.author       = { "ZYQ" => "zhaoyongqiang@agora.io" }
  spec.source       = { :git => "" }
  spec.source_files  = "**/*.swift"
  spec.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64', 'DEFINES_MODULE' => 'YES' }
  spec.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64', 'DEFINES_MODULE' => 'YES' }
  spec.ios.deployment_target = '10.0'
  spec.swift_versions = "5.0"
  spec.requires_arc  = true
  spec.static_framework = true
  spec.dependency "Zip"
end
