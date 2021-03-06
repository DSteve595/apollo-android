package com.apollographql.apollo.gradle

import com.apollographql.apollo.gradle.android.AndroidTaskConfiguratorFactory
import com.apollographql.apollo.gradle.jvm.JvmTaskConfiguratorFactory
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileResolver

import javax.inject.Inject

class ApolloPlugin implements Plugin<Project> {
  private static final String NODE_VERSION = "6.7.0"
  public static final String TASK_GROUP = "apollo"

  private Project project
  private final FileResolver fileResolver
  private boolean useGlobalApolloCodegen = System.properties['apollographql.useGlobalApolloCodegen']?.toBoolean()
  private boolean useExperimentalCodegen = (System.properties['apollographql.useExperimentalCodegen'] ?: "true").toBoolean()

  @Inject
  ApolloPlugin(FileResolver fileResolver) {
    this.fileResolver = fileResolver
  }

  @Override
  void apply(Project project) {
    this.project = project
    project.plugins.withId("java-base") {
      applyApolloPlugin()
    }
    project.gradle.getTaskGraph().whenReady {
      if (!project.plugins.hasPlugin("java-base")) {
        throw new IllegalArgumentException(
            "Apollo plugin couldn't be applied without Android or Java or Kotlin plugin.")
      }
    }
  }

  private void applyApolloPlugin() {
    if (!useGlobalApolloCodegen && !useExperimentalCodegen) {
      setupNode()
    }

    project.extensions.create(ApolloExtension.NAME, ApolloExtension, project)
    project.apollo.extensions.create(ApolloSourceSetExtension.NAME, ApolloSourceSetExtension, project)

    if (!useGlobalApolloCodegen && !useExperimentalCodegen) {
      project.tasks.create(ApolloCodegenInstallTask.NAME, ApolloCodegenInstallTask.class)
    }

    createSourceSetExtensions()
    addApolloTasks()
  }

  private void addApolloTasks() {
    Task apolloIRGenTask = project.task("generateApolloIR")
    apolloIRGenTask.group = TASK_GROUP

    Task apolloClassGenTask = project.task("generateApolloClasses")
    apolloClassGenTask.group = TASK_GROUP

    if (isAndroidProject()) {
      AndroidTaskConfiguratorFactory.create(project).configureTasks(apolloIRGenTask, apolloClassGenTask)
    } else {
      JvmTaskConfiguratorFactory.create(project).configureTasks(apolloIRGenTask, apolloClassGenTask)
    }
  }

  private void setupNode() {
    project.plugins.apply NodePlugin
    NodeExtension nodeConfig = project.extensions.findByName("node") as NodeExtension
    nodeConfig.download = true
    nodeConfig.version = NODE_VERSION
  }

  private void createSourceSetExtensions() {
    getSourceSets().all { sourceSet ->
      sourceSet.extensions.add(
          SourceDirectorySet.class,
          GraphQLSourceDirectorySet.NAME,
          GraphQLSourceDirectorySet.create(sourceSet.name, project.objects)
      )
    }
  }

  private boolean isAndroidProject() {
    return project.hasProperty('android') && project.android.sourceSets
  }

  private Object getSourceSets() {
    return (isAndroidProject() ? project.android.sourceSets : project.sourceSets)
  }
}
