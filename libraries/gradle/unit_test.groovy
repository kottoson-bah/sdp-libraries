def call(){
    stage("Gradle Unit Tests"){
        node{
            unstash "workspace"
            
            def gradle_container_image = config.gradle_container_image ?: "openjdk:8"      
            def test_reports_directory = config.test_reports_directory ?: "build/reports/tests/test/**"
            def jacoco_results_directory = config.jacoco_results_directory ?: "build/jacoco/**"
            def timeout = config.unit_test_timeout ?: 30
            
            try{
                timeout(time: 30, unit: 'MINUTES') { 
                    docker.image("${gradle_container_image}").inside {   
                        sh "./gradlew test"
                        echo "build dir contents:"
                        echo "archiving test reports"
                        archiveArtifacts allowEmptyArchive: true, artifacts: "${test_reports_directory}"
                        echo "archiving jacoco"
                        archiveArtifacts allowEmptyArchive: true, artifacts: "${jacoco_results_directory}"
                        stash "test-results" 
                    }
                }
            }catch(any){
                println "issue with gradle unit tests"
                archiveArtifacts allowEmptyArchive: true, artifacts: "${test_reports_directory}"
                archiveArtifacts allowEmptyArchive: true, artifacts: "${jacoco_results_directory}"
                error "${any.getMessage()}"
            }
        }
    }
}