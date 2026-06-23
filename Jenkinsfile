pipeline {
  agent any
  tools {
    jdk 'jdk-21'
    maven 'maven-3'
    nodejs 'node-24'
  }
  options {
    timestamps()
    disableConcurrentBuilds()
  }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Backend tests') { steps { sh './mvnw --batch-mode clean verify' } }
    stage('Dependency audit') { steps { sh 'npm audit --audit-level=high' } }
    stage('Install browser tests') {
      steps {
        sh 'npm ci'
        sh 'npx playwright install --with-deps chromium'
      }
    }
    stage('End-to-end tests') { steps { sh 'CI=true npm test' } }
    stage('Package image') {
      when { branch 'main' }
      steps { sh 'docker build -t requestflow-ai:${BUILD_NUMBER} .' }
    }
  }
  post {
    always {
      junit 'target/surefire-reports/*.xml'
      archiveArtifacts artifacts: 'target/*.jar,target/site/**,playwright-report/**,test-results/**', allowEmptyArchive: true
    }
  }
}
