/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2015 ForgeRock AS.
 */
package org.forgerock.opendj.maven.doc;

import static org.forgerock.util.Utils.*;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides utility methods for generating documentation.
 */
public final class Utils {

    /** Line separator. */
    static final String EOL = System.getProperty("line.separator");

    /**
     * Creates a directory unless it already exists.
     * @param directory     The directory to create.
     * @throws IOException  Failed to create directory.
     */
    static void createDirectory(final String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create directory: " + directory);
            }
        }
    }

    /**
     * Returns the path to the current Java executable.
     * @return The path to the current Java executable.
     */
    static String getJavaCommand() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    /**
     * Copies the content of the original file to the copy.
     * @param original      The original file.
     * @param copy          The copy.
     * @throws IOException  Failed to make the copy.
     */
    static void copyFile(File original, File copy) throws IOException {
        copyInputStreamToFile(new FileInputStream(original), copy);
    }

    /**
     * Copies the content of the original input stream to the copy.
     * @param original      The original input stream.
     * @param copy          The copy.
     * @throws IOException  Failed to make the copy.
     */
    static void copyInputStreamToFile(InputStream original, File copy) throws IOException {
        if (original == null) {
            throw new IOException("Could not read input to copy.");
        }
        if (!copy.exists() && !copy.createNewFile()) {
            throw new IOException("Failed to create " + copy);
        }
        OutputStream outputStream = new FileOutputStream(copy);
        int bytesRead;
        byte[] buffer = new byte[4096];
        while ((bytesRead = original.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }
        closeSilently(original, outputStream);
    }


    /**
     * Returns the classpath for the class loader and its parent.
     * @param classLoader   Contains the URLs of the class path to return.
     * @return The classpath for the class loader and its parent.
     */
    static String getClassPath(URLClassLoader classLoader) throws URISyntaxException {
        Set<URL> urls = new LinkedHashSet<>();
        Collections.addAll(urls, classLoader.getURLs());
        Collections.addAll(urls, ((URLClassLoader) classLoader.getParent()).getURLs());
        Set<String> paths = new LinkedHashSet<>();
        for (URL url: urls) {
            paths.add(new File(url.toURI()).getPath());
        }
        return joinAsString(File.pathSeparator, paths);
    }

    /**
     * Returns a ClassLoader including the project's runtime classpath elements.
     * This is useful when running a Java command from inside a Maven plugin.
     *
     * @param project   The Maven project holding runtime classpath elements.
     * @param log       A plugin log to use for debugging.
     * @return A ClassLoader including the project's runtime classpath elements.
     * @throws DependencyResolutionRequiredException    Failed to access the runtime classpath
     * @throws MalformedURLException                    Failed to add an element to the classpath
     */
    static URLClassLoader getRuntimeClassLoader(MavenProject project, Log log)
            throws DependencyResolutionRequiredException, MalformedURLException {
        List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
        Set<URL> runtimeUrls = new LinkedHashSet<>();
        for (String element : runtimeClasspathElements) {
            runtimeUrls.add(new File(element).toURI().toURL());
        }

        final URLClassLoader urlClassLoader = new URLClassLoader(
                runtimeUrls.toArray(new URL[runtimeClasspathElements.size()]),
                Thread.currentThread().getContextClassLoader());
        debugClassPathElements(urlClassLoader, log);
        return urlClassLoader;
    }

    /**
     * Logs what is on the classpath for debugging.
     * @param classLoader   The ClassLoader with the classpath.
     * @param log           The Maven plugin log in which to write debug messages.
     */
    static void debugClassPathElements(ClassLoader classLoader, Log log) {
        if (null == classLoader) {
            return;
        }
        log.debug("--------------------");
        log.debug(classLoader.toString());
        if (classLoader instanceof URLClassLoader) {
            final URLClassLoader ucl = (URLClassLoader) classLoader;
            int i = 0;
            for (URL url : ucl.getURLs()) {
                log.debug("url[" + (i++) + "]=" + url);
            }
        }
        debugClassPathElements(classLoader.getParent(), log);
    }

    private Utils() {
        // Not used.
    }
}
