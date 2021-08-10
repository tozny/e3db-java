package com.tozny.e3db;

/**
 * Indicates that the given realm could not be found.
 *
 * This exception can occur when getting public realm information
 * where the realm's name is not found.
 */
public class E3dbRealmNotFoundException extends E3DBException {
    /**
     * Name of the realm that was not found.
     */
    public final String realmName;

    public E3dbRealmNotFoundException(String realmName) {
        super("'" + realmName + "' not found.");
        this.realmName = realmName;
    }
}
