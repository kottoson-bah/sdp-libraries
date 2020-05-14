void call() {

  if (config.run_gradle_tasks.modules) {
    //multi-module process
    def modules = [:]

    config.run_gradle_tasks.modules.each{ module_name, module_config ->
      modules[module_name] = { invokeGradle(module_name.toString(), '', module_config) }
    }

    if (config.run_gradle_tasks.order) {
      config.run_gradle_tasks.order.each{
        stage("Running Gradle Tasks For: ${it.join(", ")}") {
          parallel modules.subMap(it)
        }
      }
    } else {
        stage("Running Gradle Tasks") {
          parallel modules
        }
    }
  } else {
    //single module process
    stage("Running Gradle Tasks") {
        invokeGradle()
    }
  }

}