// ${JOB_NAME}
import com.internship.tls.Shared

def call() {

    String job = "${JOB_NAME}".split('_')[-1];
    String rep = "${job}".split('/')[0].toLowerCase()
    def configs  = readJSON text: libraryResource("app/${rep}.json")
    def shared = new Shared(this)

    pipeline {
        agent {
            node {
                label 'training-licenses-sharing-cloud'    
            }
        }

        tools {
            maven 'maven-3.8.6' 
            nodejs 'nodejs-17.9.1'
        }

        environment {
            // environment for nexus
            projectName = "${JOB_NAME}".split('/')[1].replace("_", "-").toLowerCase();
            registryCredentials = "training-licenses-nexus"
            registry = "nexus.demo.think-it.work:6666/"
            // Git
            gitURL = "${configs.projectURL}"
            gitTagsFile = 'tagsFromGit.json'
            // Project Version
            projectVersion = shared.getVersion((String)configs.version.file)
            tag = "${projectVersion}.${BUILD_NUMBER}"
            // environment for frontent unit test
            CHROME_BIN = '/usr/bin/chromium-browser'
        }

        stages {
            stage('Check version') {
                when {
                    branch 'MR-*'
                }
                steps {
                    echo "Check that version is higher than version from master"
                    script{
                        // Extract tags from main branch
                        withCredentials([string(credentialsId: 'mircea_git_api', variable: 'apiKey')]) {
                            sh "curl --request GET --header 'PRIVATE-TOKEN: ${apiKey}' 'https://gitlab.demo.think-it.work/api/v4/projects/${shared.gitPathUrl((String)gitURL)}/repository/tags/' > ${gitTagsFile}"
                        }
                        // Get Versions
                        versionFromGit = shared.getVersionFromMain((String)gitTagsFile)
                        projectCurentVersion = projectVersion.replace('.', '') as int
                        echo "Version from main: ${versionFromGit}"
                        echo "Curent Version: ${projectCurentVersion}"
                        
                        // Check versions
                        if (projectCurentVersion <= versionFromGit) {
                            throw new Exception("ERR_VERSION")
                        }
                    }
                }
            }
            stage('BUILD') {
                steps {
                    echo "BUILD "
                    script{
                        sh "${configs.buildApp}"
                    }
                }
            }
            // Test steps
            stage('Unit tests') {
                steps {
                    //  "Unit tests"
                    echo "Unit tests"
                    script{
                        sh "${configs.unitTests}"
                    }
                }
            }
            stage('Integration tests') {
                when {
                    branch 'MR-*'
                }
                steps {
                    sh "${configs.integrationTests}"
                }
            }
            stage('Sonarqube') {
                steps {
                    echo "Check with Sonarqube"
                    script{
                        if ("${BRANCH_NAME}".contains('MR-')){
                            projectName = "${projectName}-mr"
                        }
                        withEnv(["projectName=${projectName}"]) {
                            withSonarQubeEnv('SonarQube Server') {
                                sh "${configs.checkInSonar}"
                            }
                        }
                        
                        timeout(1) {
                            waitForQualityGate abortPipeline: true
                        }
                        
                    } 
                }
            }
            stage('Create a docker image') {
                when {
                    branch 'main'
                }
                steps {
                    echo "Create a docker image"
                    // "Create a docker image with the artifact"
                    script{
                        dockerImage = docker.build projectName
                    }
                   
                }
            }
            stage('Push the image to Nexus') {
                when {
                    branch 'main'
                }
                steps {
                    //  "Create a tag with version into repository. (version from pom + jenkins build number)"
                    echo "Push to nexus"
                    script {
                        docker.withRegistry( 'https://' + registry, registryCredentials ) {
                            dockerImage.push("${tag}")
                            dockerImage.push("latest")
                        }
                    }
                }
            }
            stage('Create a tag with version into repository') {
                when {
                    branch 'main'
                }
                steps {
                    echo "Create a tag with version into repository. (version from pom + jenkins build number)"
                    script{
                        gitUrlNoProtocol = "${gitURL}".split('//')[1]
                    }
                   
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', 
                                    credentialsId: 'jenkins_gitlab', 
                                    usernameVariable: 'GIT_USERNAME', 
                                    passwordVariable: 'GIT_PASSWORD']]) {
                        sh("git tag -a ${tag} -m 'Jenkins'")
                        sh("git push https://$GIT_USERNAME:$GIT_PASSWORD@${gitUrlNoProtocol} --tags")
                        sh("git tag -d ${tag}")
                    }
                }
            }
         
        }
    }
}
