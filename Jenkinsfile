pipeline {
agent any
tools {
  maven 'Maven'
}
stages {
stage('Checkout') {
steps {
git 'https://github.com/naddri/OhjelmistoTuotantoProjekti1'
}
}
stage('Build') {
steps {
bat 'mvn clean install' // sh for linux and ios
}
}
stage('Test') {
steps {
bat 'mvn test'
}
}
stage('Code Coverage') {
steps {
bat 'mvn jacoco:report'
}
}
stage('Publish Test Results') {
steps {
junit '**/target/surefire-reports/*.xml'
}
}
stage('Publish Coverage Report') {
steps {
jacoco()
}
}
}
}
