@Library('jenkins-shared-libs@ID-30289') _
def config = [ appName: 'lti-launch',
               podName: 'java-11-maven-3.5.2.yaml',
               containerName: 'jdk-11-maven',
               runUnitTests: false,
               runIntegrationTests: true
             ]
javaPipeline(config)
