/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.dingxin.maven.plugins.proguard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Maven Plugin for ProGuard
 * 
 * @author dingxin
 */
@Mojo(name = "proguard", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ProguardMojo extends AbstractMojo {

	/**
	 * Disables the plugin execution
	 */
	@Parameter(property = "maven.proguard.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * ProGuard options
	 */
	@Parameter
	private String[] options;

	/**
	 * ProGuard configuration file
	 */
	@Parameter(defaultValue = "proguard.conf")
	private File configFile;

	/**
	 * Directory containing the input and generated JAR.
	 */
	@Parameter(property = "project.build.directory")
	private File targetDirectory;

	/**
	 * Specifies the input jar name (or war, apk) of the application to be
	 * processed.
	 *
	 * You may specify a classes directory, e.g. 'classes'.
	 */
	@Parameter
	private String injar;

	/**
	 * Specifies the name of the output jar (or war, apk)
	 */
	@Parameter
	private String outjar;

	/**
	 * ProGuard Filters for the input jar
	 * 
	 * e.g. !module-info.class,!META-INF/maven/**
	 */
	@Parameter(defaultValue = "!module-info.class,!META-INF/maven/**")
	private String inFilter;

	/**
	 * ProGuard Filters for the output jar
	 * 
	 * e.g. !META-INF/maven/**
	 */
	@Parameter(defaultValue = "!META-INF/maven/**")
	private String outFilter;

	/**
	 * Add dependency jars to -libraryjars arguments
	 */
	@Parameter(defaultValue = "true")
	private boolean includeDependency;

	/**
	 * Add dependency jars to -injars arguments
	 */
	@Parameter(defaultValue = "false")
	private boolean includeDependencyInjar;

	/**
	 * ProGuard Filters for dependency jars
	 * 
	 * e.g. !module-info.class
	 */
	@Parameter(defaultValue = "!module-info.class,!META-INF/**")
	private String dependencyFilter;

	/**
	 * Additional -libraryjars
	 * 
	 * e.g. ${java.home}/jmods/java.base.jmod(!**.jar;!module-info.class)
	 * 
	 * e.g. ${java.home}/lib/rt.jar
	 */
	@Parameter
	private List<String> libs;

	/**
	 * The Maven project reference where the plugin is currently being executed.
	 */
	@Parameter(property = "project", readonly = true)
	private MavenProject mavenProject;

	/**
	 * The plugin dependencies.
	 */
	@Parameter(property = "plugin.artifacts", readonly = true)
	private List<Artifact> pluginArtifacts;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Proguard is skipped.");
			return;
		}

		if (injar == null) {
			return;
		}

		setDefaultJarName();

		List<String> args = getArgs();
		execProguard(args);
	}

	private void execProguard(List<String> args) throws MojoExecutionException, MojoFailureException {
		getLog().info("Execute ProGuard: " + args.toString());

		File proguardJar = getProguardJar();

		Project antProject = new Project();
		antProject.setName(this.mavenProject.getName());
		antProject.init();

		DefaultLogger antLogger = new DefaultLogger();
		antLogger.setOutputPrintStream(System.out);
		antLogger.setErrorPrintStream(System.err);
		antLogger.setMessageOutputLevel(Project.MSG_INFO);

		antProject.addBuildListener(antLogger);
		antProject.setBaseDir(this.mavenProject.getBasedir());

		Java java = new Java();
		java.setProject(antProject);
		java.setTaskName("proguard");
		java.createClasspath().setLocation(proguardJar);
		java.setClassname("proguard.ProGuard");
		java.setFailonerror(true);
		java.setFork(true);

		for (String arg : args) {
			java.createArg().setValue(arg);
		}

		int result = java.executeJava();
		if (result != 0) {
			throw new MojoExecutionException("ProGuard failed (result=" + result + ")");
		}
	}

	private void setDefaultJarName() {
		String type = mavenProject.getPackaging();
		getLog().debug("Package Type: " + type);

		if ("jar".equals(type)) {
			if (injar == null) {
				injar = mavenProject.getBuild().getFinalName() + ".jar";
			}
			if (outjar == null) {
				outjar = injar;
			}
		} else if ("war".equals(type)) {
			if (injar == null) {
				injar = "classes";
			}
			if (outjar == null) {
				outjar = injar;
			}
		}

		getLog().debug("injar " + injar);
		getLog().debug("outjar " + outjar);
	}

	private File getInjarFile() throws MojoFailureException {
		File injarFile = new File(targetDirectory, injar);
		if (!injarFile.exists()) {
			throw new MojoFailureException("Can't find file " + injarFile);
		}

		File tempInjarFile;
		if (injarFile.isDirectory()) {
			tempInjarFile = new File(targetDirectory, nameNoExt(injar) + "_proguard_base");
		} else {
			tempInjarFile = new File(targetDirectory, nameNoExt(injar) + "_proguard_base.jar");
		}
		if (tempInjarFile.exists() && !deleteFile(tempInjarFile)) {
			throw new MojoFailureException("Can't delete " + tempInjarFile);
		}
		if (!injarFile.renameTo(tempInjarFile)) {
			throw new MojoFailureException("Can't rename " + injarFile);
		}
		injarFile = tempInjarFile;

		getLog().debug("injarFile " + injarFile);

		return injarFile;
	}

	private File getOutjarFile() throws MojoFailureException {
		File outjarFile = new File(targetDirectory, outjar);

		if (outjarFile.exists() && !deleteFile(outjarFile)) {
			throw new MojoFailureException("Can't delete " + outjarFile);
		}

		return outjarFile;
	}

	private File getProguardJar() throws MojoFailureException {
		for (Artifact artifact : this.pluginArtifacts) {
			getLog().debug("Plugin Artifact: " + artifact.getFile());
			if (artifact.getArtifactId().equals("proguard-base")) {
				getLog().debug("Proguard Artifact: " + artifact.getFile());
				return artifact.getFile().getAbsoluteFile();
			}
		}
		throw new MojoFailureException("ProGuard not found");
	}

	private List<String> getArgs() throws MojoFailureException {
		List<String> args = new ArrayList<>();
		argForConfigFile(args);
		argForInjar(args);
		argForOutjar(args);
		argForLibs(args);
		argForDependency(args);
		argForOptions(args);

		return args;
	}

	private void argForConfigFile(List<String> args) {
		if (configFile != null && configFile.exists()) {
			args.add("-include");
			args.add(configFile.getAbsolutePath());
			getLog().debug("ProGuard Configuration File: " + configFile);
		}
	}

	private void argForInjar(List<String> args) throws MojoFailureException {
		args.add("-injars");

		String classpath = getInjarFile().getAbsolutePath();
		String filter = "";
		if (inFilter != null) {
			filter = "(" + inFilter + ")";
		}

		args.add(classpath + filter);
	}

	private void argForOutjar(List<String> args) throws MojoFailureException {
		args.add("-outjars");

		String classpath = getOutjarFile().getAbsolutePath();
		String filter = "";
		if (outFilter != null) {
			filter = "(" + outFilter + ")";
		}

		args.add(classpath + filter);
	}

	private void argForLibs(List<String> args) {
		if (libs != null) {
			for (String lib : libs) {
				args.add("-libraryjars");
				args.add(lib);
			}
		}
	}

	private void argForDependency(List<String> args) {
		if (!includeDependency) {
			return;
		}

		String option = "-libraryjars";
		if (includeDependencyInjar) {
			option = "-injars";
		}

		String filter = "";
		if (dependencyFilter != null) {
			filter = "(" + dependencyFilter + ")";
		}

		Set<Artifact> artifacts = this.mavenProject.getArtifacts();
		for (Artifact artifact : artifacts) {
			String classpath = artifact.getFile().getAbsolutePath();
			args.add(option);
			args.add(classpath + filter);
		}
	}

	private void argForOptions(List<String> args) {
		if (options != null) {
			Collections.addAll(args, options);
		} else {
			// Default options
			args.add("-dontoptimize");// 不要优化
			args.add("-keepattributes *Annotation*");// 避免混淆注解
			args.add("-keepattributes Signature");// 避免混淆泛型
			args.add("-keepattributes InnerClasses");// 避免混淆内部类
			args.add("-keepclassmembers class * { @**.* *; }");// 避免混淆带注解的成员
			args.add("-keep public class * { public protected *; }");// 避免混淆公开的成员
		}
	}

	private String nameNoExt(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			return fileName;
		} else {
			return fileName.substring(0, dotIndex);
		}
	}

	private boolean deleteFile(File file) {
		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				deleteFile(subFile);
			}
		}
		return file.delete();
	}

}
