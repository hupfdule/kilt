/*******************************************************************************
 * Copyright 2012 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.poiu.kilt.maven;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import de.poiu.kilt.internal.XlsImExporter;


/**
 * Exports the translations in the resource bundle files into an XLS file.
 */
@Mojo(name = "export-xls")
public class ExportXlsMojo extends AbstractKiltMojo {

  /////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  /**
   * Location of the output directory root.
   */
  @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}", required = true)
  private File xlsOutputDirectory;


  @Parameter(property = "xlsFileEncoding", defaultValue = "UTF-8")
  private String xlsFileEncoding;


  @Parameter(property = "xlsFileName", required= true, defaultValue = "i18n.xls")
  private String xlsFileName;


  /////////////////////////////////////////////////////////////////////////////
  //
  // Constructors


  /////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  @Override
  public void execute() throws MojoExecutionException {
    this.getLog().info("Exporting properties to XLS.");

    final Set<File> propertyFileSet = this.getIncludedPropertyFiles(this.propertiesRootDirectory);

    try {
      Files.createDirectories(this.xlsOutputDirectory.toPath());
      final File file = new File(this.xlsOutputDirectory, this.xlsFileName);

      XlsImExporter.exportXls(this.propertiesRootDirectory.toPath(),
                                propertyFileSet,
                                this.propertyFileEncoding,
                                file.toPath(),
                                this.xlsFileEncoding);
    } catch (IOException e) {
      throw new RuntimeException("Error exporting property files to XLS.", e);
    }

    this.getLog().info("...done");
  }


  private Set<File> getIncludedPropertyFiles(final File propertiesRootDirectory) {
    if (!propertiesRootDirectory.exists()) {
      this.getLog().warn("resource bundle directory " + propertiesRootDirectory + " does not exist. Nothing will be exported.");
      return ImmutableSet.of();
    }

    final Set<File> matchingFiles = new LinkedHashSet<>();

    final DirectoryScanner directoryScanner = new DirectoryScanner();
    directoryScanner.setIncludes(this.i18nIncludes);
    directoryScanner.setExcludes(this.i18nExcludes);
    directoryScanner.setBasedir(propertiesRootDirectory);
    directoryScanner.scan();

    final String[] fileNames = directoryScanner.getIncludedFiles();
    for (String fileName : fileNames) {
      if (this.verbose) {
        this.getLog().info("Including in XLS: " + fileName);
      }
      matchingFiles.add(new File(propertiesRootDirectory, fileName));
    }

    if (matchingFiles.isEmpty()) {
      this.getLog().warn("No resource bundles found. Nothing will be exported.");
    }

    return matchingFiles;
  }

}