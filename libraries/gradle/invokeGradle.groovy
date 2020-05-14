void call(module_name = '', phase = '', module_config = config.run_gradle_tasks) {

  node{
    //clean and prep workspace
    cleanWs()
    unstash "workspace"
    
    def container_image = config.container_image ?: 'openjdk:8'
    
    docker.image(config.container_image).inside {

      //reset module_name for single-module projects
      module_name = module_name ?: '.'
      
      if (!fileExists(module_name)) {
        error "Module ${module_name} does not exist." 
      }

      def block = {
        //get tasks
        String tasks = ""
        if (module_config.tasks) {
          tasks = module_config.tasks.join(" ")
        }else if(phase && config.get(phase)?.tasks){
          tasks = config.get(phase)?.tasks.join(" ")
        }else if (config.run_gradle_tasks.tasks) {
          tasks = config.run_gradle_tasks.tasks.join(" ")
        }

        //get task exclusions
        String tasks_exclude = ""
        if (module_config.tasks_exclude) {
          tasks_exclude = module_config.tasks_exclude.collect{ "-x ${it} " }.join(" ")
        }else if(phase && config.get(phase)?.tasksExclude){
          tasksExclude = config.get(phase)?.tasksExclude.collect{ "-x ${it} " }.join(" ")
        }else if (config.run_gradle_tasks.tasks_exclude) {
          tasks_exclude = config.run_gradle_tasks.tasks_exclude.collect{ "-x ${it} " }.join(" ")
        }

        //get flags
        String flags = ""
        if (module_config.flags) {
          flags = module_config.flags.join(" ")
        } else if (phase && config.get(phase)?.flags){
          flags = config.get(phase)?.flags.join(" ")
        }else if (config.run_gradle_tasks.flags) {
          flags = config.run_gradle_tasks.flags.join(" ")
        }

        //build gradle call
        String gradle_call = "./gradlew ${flags} ${tasks} ${tasks_exclude} "

        //add sonarqube flags
        if (tasks.contains('sonarqube')) {
          String sonar_properties = "sonarqube "
          String project_key = ""
          String project_name = ""

          String sonar_url = module_config.sonar_host ?: (config.run_gradle_tasks.sonar_host ?: "http://localhost:9000")

          sonar_properties += "-Dsonar.host.url=${sonar_url} " +
                         "-Dsonar.login=${SONAR_USERNAME} " +
                         "-Dsonar.password=${SONAR_TOKEN} " +
                         "-Dsonar.exclusions=**/src/test/**/* "

          if (module_name != ".") {
            project_key  = "$env.REPO_NAME-$module_name::$env.BRANCH_NAME".replaceAll("/", "_")
            project_name = "$env.REPO_NAME/$module_name ($env.BRANCH_NAME)"
            sonar_properties += "-Dsonar.projectBaseDir=.. "
          } else {
            project_key  = "$env.REPO_NAME::$env.BRANCH_NAME".replaceAll("/", "_")
            project_name = "$env.REPO_NAME ($env.BRANCH_NAME)"
          }

          sonar_properties += "-Dsonar.projectKey=\"$project_key\" " +
                          "-Dsonar.projectName=\"$project_name\" "

          gradle_call = gradle_call.split("sonarqube")[0] +
                        sonar_properties +
                        gradle_call.split("sonarqube")[1]
        }

        //run it
        try {
          dir(module_name) {
            sh gradle_call
          }

        } catch (any) {

          enforce = module_config.subMap("enforce") ? config.enforce : true

          if (enforce) {
            error "${gradle_call} failed - ${any.getMessage()}"
          } else {
            unstable "${gradle_call} failed - ${any.getMessage()}"
          }

        } finally {

          //show test results
          def test_results = module_config.test_results ?: config.get(phase)?.artifacts ?: config.run_gradle_tasks.test_results ?: null
          if (test_results) {
            test_results.each{
              try {
                sh script: "touch ${it}", returnStatus: true 
                junit it
              } catch(any) {
                println "error gathering JUnit test results from ${it}"
              }
            }
          }

          //archive artifacts
          def artifacts = module_config.artifacts ?: config.get(phase)?.artifacts ?: config.run_gradle_tasks.artifacts ?: null
          if (artifacts) {
            artifacts.each{
              try{ 
                archiveArtifacts it 
              } catch(any) {
                println "error Archiving ${it}"
              }
            }
          }
        }
      }
      
      def credentials_config = config.get(phase)?.credentials ?: config.run_gradle_tasks.credentials ?: null
      if (credentials_config) {
        //build credential block
        def creds = []
        credentials_config.each { key, value ->
          creds.push(usernamePassword(credentialsId: value.id, 
                     passwordVariable: value.passwordVar, 
                     usernameVariable: value.usernameVar))
        }

        withCredentials(creds, block)
      } else {
        block()
      }
    }

    //final workspace cleanup
    cleanWs()
  }
}