/*
 * Copyright (C) 2018 Marco Herrn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnaest.i18nbinder.internal;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.io.File;
import java.util.Map;
import org.omnaest.i18nbinder.facade.creation.InconsistentBundleBaseNameException;
import org.omnaest.utils.propertyfile.PropertyFile;
import org.omnaest.utils.propertyfile.content.PropertyMap;
import org.omnaest.utils.propertyfile.content.element.Property;


/**
 * A holder for all information related to a localization resource bundle.
 * <p>
 * It contains a name for the bundle (actually the bundleBasename) and all keys with their
 * translations.
 * <p>
 * This is a thread-safe immutable class.
 * <p>
 * This class provides a fluent interface. An example to create it and fill it with translations
 * is:
 * <pre>
 *   final FacadeBundleContent facadeBundleContent= FacadeBundleContent.forName("myBundle")
 *                                                                     .fromFriles(myFilesMap);
 * </pre>
 *
 * To create the map with the content files, the class {@link FacadeBundleContentHelper} can be used.
 *
 * @author mherrn
 */
public class ResourceBundleContent {


  /////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  /** The bundleBasename of this bundle. */
  private final String bundlBaseName;

  /** All keys of this bundle and their available translations. */
  private final Multimap<String, Translation> content= LinkedListMultimap.create();


  /////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  /**
   * Creates a new FacadeBundleContent for the given bundle with the given translations.
   *
   * @param bundleBaseName the bundleBasename
   * @param content all keys and their translations for this bundle
   */
  private ResourceBundleContent(final String bundleBaseName, final Multimap<String, Translation> content) {
    this.bundlBaseName= bundleBaseName;
    //FIXME: Sollte das irgendwie sortiert sein? Alphabetisch? Oder nach der gelesenen Reihenfolge?
    this.content.putAll(content);
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Methods


  /**
   * Returns the bundleBasename.
   *
   * @return the bundleBasename
   */
  public String getBundleBaseName() {
    return bundlBaseName;
  }


  /**
   * Returns the keys and their translations for this bundle.
   *
   * @return a Multimap containing the translated keys as the keys in the map and all translations
   * for that key as the corresponding values.
   */
  public Multimap<String, Translation> getContent() {
    return content;
  }


  public void addTranslation(final String bundleKey, final Translation translation) {
    this.content.put(bundleKey, translation);
  }


  //FIXME: forName und fromFiles sollte in eine Methode kombiniert werden.
  //       Ich kriege ja immer beides aus dem Helper zur[ck

  /**
   * Creates a new FacadeBundleContent for the given bundle without any translations.
   * To fill it with translations use the method {@link #fromFiles(java.util.Map) ).
   *
   * @param bundleName the bundleBasename
   * @return a new FacadeBundleContent for the the given bundle
   */
  public static ResourceBundleContent forName(final String bundleName) {
    final ImmutableMultimap<String, Translation> emptyMap= ImmutableMultimap.of();
    return new ResourceBundleContent(bundleName, emptyMap);
  }


  /**
   * Returns a new FacadeBundleContent derived from this one and set its translations
   * based on the given map of bundle files.
   *
   * @param bundleFiles the file containing the translations for this bundle for each language
   * @return a FacadeBundleContent with the translations from the given files
   * @throws InconsistentBundleBaseNameException if thegiven files don't share a common basename
   */
  public ResourceBundleContent fromFiles(final Map<Language, File> bundleFiles) {
    final SetMultimap<String, Translation> translations= MultimapBuilder.hashKeys().linkedHashSetValues().build();

    for (final Map.Entry<Language, File> entry : bundleFiles.entrySet()) {
      final Language lang = entry.getKey();
      final File file = entry.getValue();

      final PropertyFile propertyFile = new PropertyFile(file);
      //FIXME: Set encoding
      //propertyFile.setFileEncoding(fileEncoding);
      propertyFile.setUseJavaStyleUnicodeEscaping(true);
      propertyFile.load();
      final PropertyMap propertyMap = propertyFile.getPropertyFileContent().getPropertyMap();

      propertyMap.forEach((String key, Property value) -> {

        translations.put(key, new Translation(lang, Joiner.on(",").join(value.getValueList())));
      });
    }

    return new ResourceBundleContent(this.bundlBaseName, translations);
  }


  @Override
  public String toString() {
    return "FacadeBundleContent{" + "bundleName=" + bundlBaseName + "\n\t, content=" + content + '}';
  }


}