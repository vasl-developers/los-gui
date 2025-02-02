/*
 * Copyright (c) 2000-2003 by David Sullivan
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASL.LOSGUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

import static VASL.LOSGUI.LOSEditorApp.writeError;

/**
 * Title:        LOSEditorProperties.java
 * Copyright:    Copyright (c) 2001 David Sullivan Zuericher Strasse 6 12205 Berlin Germany. All rights reserved.
 *
 * @author David Sullivan
 * @version 1.0
 */
public class LOSEditorProperties {

    // properties
    private static String BoardDirectory;


    static {
        try {
            // Get the URL of the JAR file
            URL jarUrl = LOSEditorProperties.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl == null) {
                throw new IOException("Unable to determine JAR file location");
            }

            // Construct the path to the bdFiles directory
            File jarFile = new File(jarUrl.toURI());
            File bdFilesDir = new File(jarFile.getParentFile(), "classes/bdFiles");
            BoardDirectory = bdFilesDir.getAbsolutePath();
        } catch (IOException | URISyntaxException e) {
            writeError("Unable to load boards directory from resources: " + e.getMessage());
        }
    }

    public static String getBoardDirectory() {
        return BoardDirectory;
    }
}