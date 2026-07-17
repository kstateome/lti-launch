@Library('jenkins-shared-libs') _
def config = [ appName: 'lti-launch',
               podName: 'java-21-maven-3.9.9.yaml',
               containerName: 'jdk-21-maven',
               runUnitTests: false,
               runIntegrationTests: true,
               runSonar: true
             ]
javaPipeline(config)
