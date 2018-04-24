/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db;

/**
 * Manages saving, loading and removing client
 * configurations. See {@code AndroidConfigStore} for a
 * concrete implementation.
 */
public interface ConfigStore {

  /**
   * Communicates the results of saving a config.
   */
  interface SaveHandler {
    /**
     * Called when the config was saved successfully.
     */
    void saveConfigDidSucceed();

    /**
     * Called when the save was cancelled.
     */
    void saveConfigDidCancel();

    /**
     * Called when an error occurred while saving.
     * @param e The exception that occurred.
     */
    void saveConfigDidFail(Throwable e);
  }

  /**
   * Communicates the results of loading a config.
   */
  interface LoadHandler {
    /**
     * Calle when the config was loaded.
     * @param config The config.
     */
    void loadConfigDidSucceed(String config);

    /**
     * Called when the load was cancelled.
     */
    void loadConfigDidCancel();

    /**
     * Called when the config could not be found.
     */
    void loadConfigNotFound();

    /**
     * Called when config failed to load.
     * @param e The exception that occurred.
     */
    void loadConfigDidFail(Throwable e);
  }

  /**
   * Communicates the results of removing a config.
   */
  interface RemoveHandler {
    /**
     * Called when the config was successfully removed.
     */
    void removeConfigDidSucceed();

    /**
     * Called when the config could not be removed.
     * @param e The exception that occurred.
     */
    void removeConfigDidFail(Throwable e);
  }

  /**
   * Save a configuration.
   * @param config Config to save.
   * @param handler Result handler.
   */
  void save(String config, SaveHandler handler);

  /**
   * Load a configuration.
   * @param handler Result handler.
   */
  void load(LoadHandler handler);

  /**
   * Remove a configuration.
   * @param handler Result handler.
   */
  void remove(RemoveHandler handler);
}
