package com.appland.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarFile

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

class ShadowRelocation extends DefaultTask {

  @Input
  ShadowJar target

  @Input
  String prefix = "shadow"

  @Input
  String[] excludes

  @InputFiles @Optional
  List<Configuration> getConfigurations() {
      return target.configurations
  }

  @TaskAction
  void configureRelocation() {
    def packages = [] as Set<String>

    configurations.each { configuration ->
      configuration.files.each { jar ->
        JarFile jf = new JarFile(jar)
        jf.entries().each { entry ->
          if (entry.name.endsWith(".class")) {
            packages << entry.name[0..entry.name.lastIndexOf('/')-1].replaceAll('/', '.')
          }
        }
        jf.close()
      }
    }

    packages.each {
      Boolean excluded = false
      for (String exclusion in excludes) {
        if (it.startsWith(exclusion)) {
          excluded = true
          break
        }
      }
      
      if (excluded) return

      target.relocate(it, "${prefix}.${it}")
    }
  }

  static String taskName(Task task) {
    return "configureRelocation${task.name.capitalize()}"
  }

}