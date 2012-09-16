/*
 * Copyright (c) Mark Derricutt 2010.
 *
 * The use and distribution terms for this software are covered by the Eclipse Public License 1.0
 * (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html
 * at the root of this distribution.
 *
 * By using this software in any fashion, you are agreeing to be bound by the terms of this license.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.theoryinpractise.clojure;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME )
public class ClojureRunMojo extends AbstractClojureCompilerMojo {

    /**
     * The main clojure script to run
     */
    @Parameter(property = "clojure.script")
    private String script;

    /**
     * Additional scripts to run
     */
    @Parameter
    private String[] scripts;

    /**
     * The fully qualified name of the main class to run. This main class is intended
     * to be one generated by :gen-class, although any Java class with a standard main()
     * function will do.
     */
    @Parameter(property = "clojure.mainClass")
    private String mainClass;

    /**
     * args specified on the command line.
     */
    @Parameter(property = "clojure.args")
    private String args;

    /**
     * Returns either a path to a temp file that loads all of the provided scripts,
     * or simply returns the singular <code>script</code> String (which therefore allows
     * for @ classpath-loading paths to be passed in as a script).
     * <p/>
     * If multiple scripts are defined, they must all exist; otherwise an exception is thrown.
     */
    private static String mergeScripts(String script, String[] scripts) throws MojoExecutionException {
        if (script == null || script.trim().equals("")) {
            throw new MojoExecutionException("<script> is undefined");
        }
        if (scripts == null) {
            return script;
        } else if (scripts.length == 0) {
            throw new MojoExecutionException("<scripts> is defined but has no <script> entries");
        }

        List<String> paths = new ArrayList<String>();
        paths.add(script);

        paths.addAll(Arrays.asList(scripts));
        for (String scriptFile : paths) {
            if (scriptFile == null || scriptFile.trim().equals("")) {
                throw new MojoExecutionException("<script> entry cannot be empty");
            }
            if (!(new File(scriptFile).exists())) {
                throw new MojoExecutionException(scriptFile + " cannot be found");
            }
        }

        try {
            File testFile = File.createTempFile("run", ".clj");
            final FileWriter writer = new FileWriter(testFile);

            for (String scriptFile : paths) {
                writer.write("(load-file \"" + scriptFile + "\")");
                writer.write(System.getProperty("line.separator"));
            }
            writer.close();
            return testFile.getPath();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public void execute() throws MojoExecutionException {
        if (script != null && mainClass != null) {
            throw new MojoExecutionException("Specify either 'script' or 'mainClass - not both.");
        }
        if (script == null && mainClass == null) {
            throw new MojoExecutionException("Specify either 'script' or 'mainClass'.");
        }

        try {
            if (script != null) {
                String path = mergeScripts(script, scripts);

                List<String> clojureArguments = new ArrayList<String>();
                clojureArguments.add(path);

                if (args != null) {
                    clojureArguments.addAll(Arrays.asList(args.split(" ")));
                }

                getLog().debug("Running clojure:run against " + path);

                callClojureWith(
                        getSourceDirectories(SourceDirectory.COMPILE),
                        outputDirectory, getRunWithClasspathElements(), "clojure.main",
                        clojureArguments.toArray(new String[clojureArguments.size()]));
            } else if (mainClass != null) {
                callClojureWith(
                        getSourceDirectories(SourceDirectory.COMPILE),
                        outputDirectory, getRunWithClasspathElements(), mainClass,
                        args == null ? new String[0] : args.split(" "));
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
