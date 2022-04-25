@Library('jenkins-shared-libs') _
def config = [ appName: 'lti-scantron',
               podName: 'java-11-maven-3.5.2.yaml',
               containerName: 'jdk-11-maven',
               testEnv: 'lti',
               runUnitTests: false,
               runIntegrationTests: true
             ]
javaPipeline(config)
