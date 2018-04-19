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
package org.maven.i18nbinder.plugin;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.omnaest.i18nbinder.internal.LocaleFilter;
import org.omnaest.i18nbinder.internal.ModifierHelper;
import org.omnaest.i18nbinder.internal.xls.XLSFile;


/**
 * Goal which executes the i18nBinder xls file generation
 *
 * @author <a href="mailto:awonderland6@googlemail.com">Danny Kunz</a>
 */
@Mojo(name = "export-xls")
public class ExportXlsMojo extends AbstractMojo {

  /**
   * Location of the output directory root.
   */
  @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}", required = true)
  private File xlsOutputDirectory;

  /**
   * Location of the source i18n files.
   */
  @Parameter(property = "propertiesRootDirectory", defaultValue = "src/main/resources/i18n")
  private File propertiesRootDirectory;

  @Parameter(property = "verbose", defaultValue = "false")
  private boolean verbose;

  //FIXME: Should be taken from jaxb2 maven plugin
//  @Parameter
  private String[] i18nIncludes;

//  @Parameter
  private String[] i18nExcludes;

  @Parameter(property = "propertyFileEncoding")
  private String propertyFileEncoding;

  @Parameter(property = "xlsFileEncoding", defaultValue = "UTF-8")
  private String xlsFileEncoding;

  @Parameter(property = "xlsFileName", defaultValue = "i18n.xls")
  private String xlsFileName;


  private String localeFilterRegex = ".*";

  private String fileNameLocaleGroupPattern = ".*?((_\\w{2,3}_\\w{2,3})|(_\\w{2,3})|())\\.properties";

  private List<Integer> fileNameLocaleGroupPatternGroupIndexList = Arrays.asList(2, 3, 4);

  private boolean useJavaStyleUnicodeEscaping = true;


  /* *************************************************** Methods **************************************************** */

  @Override
  public void execute() throws MojoExecutionException {
    this.getLog().info("Create XLS file from property files...");
    this.logConfigurationProperties();

    final LocaleFilter localeFilter = this.determineLocaleFilter();
    final Set<File> propertyFileSet = this.resolveFilesFromDirectoryRoot(this.propertiesRootDirectory);

    try {
      if (this.xlsFileName != null && !propertyFileSet.isEmpty()) {
        XLSFile xlsFile = ModifierHelper.createXLSFileFromPropertyFiles(this.propertiesRootDirectory.toPath(),
                                                                        propertyFileSet, this.propertyFileEncoding,
                                                                        localeFilter, this.fileNameLocaleGroupPattern,
                                                                        this.fileNameLocaleGroupPatternGroupIndexList,
                                                                        this.useJavaStyleUnicodeEscaping);

        Files.createDirectories(this.xlsOutputDirectory.toPath());
        File file = new File(this.xlsOutputDirectory, this.xlsFileName);
        xlsFile.setFile(file);
        xlsFile.store();

      } else {
        this.getLog().error("No xls file name specified. Please provide a file name for the xls file which should be created.");
      }

    } catch (IOException e) {
      this.getLog().error("Could not write xls file", e);
    }

    this.getLog().info("...done");
  }


  /**
   *
   */
  private void logConfigurationProperties() {
    this.getLog().info("fileNameLocaleGroupPattern=" + this.fileNameLocaleGroupPattern);
    this.getLog().info("fileNameLocaleGroupPatternGroupIndexList=" + this.fileNameLocaleGroupPatternGroupIndexList);
    this.getLog().info("localeFilterRegex=" + this.localeFilterRegex);
    this.getLog().info("xlsOutputDirectory=" + this.xlsOutputDirectory);
    this.getLog().info("xlsFileName=" + this.xlsFileName);
    this.getLog().info("propertiesRootDirectory=" + this.propertiesRootDirectory);
    this.getLog().info("useJavaStyleUnicodeEscaping=" + this.useJavaStyleUnicodeEscaping);
    this.getLog().info("propertyFileEncoding=" + this.propertyFileEncoding);
  }


  private Set<File> resolveFilesFromDirectoryRoot(final File propertiesRootDirectory) {
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
    for (int i = 0; i < fileNames.length; i++) {
      if (this.verbose) {
        this.getLog().info("Including in XLS: " + fileNames[i]);
      }
      matchingFiles.add(new File(propertiesRootDirectory, fileNames[i]));
    }

    if (matchingFiles.isEmpty()) {
      this.getLog().warn("No resource bundles found. Nothing will be exported.");
    }

    return matchingFiles;
  }


  private LocaleFilter determineLocaleFilter() {
    final LocaleFilter localeFilter = new LocaleFilter();
    localeFilter.setPattern(Pattern.compile(this.localeFilterRegex));
    return localeFilter;
  }

}
