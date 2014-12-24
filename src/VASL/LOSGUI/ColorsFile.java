package VASL.LOSGUI;

/**
 * Copyright (c) 2014 by David Sullivan
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Wraps the colorSSR file in the board archive, loads the transformations into private classes, prints as XML
 */
public class ColorsFile {

    // the name for colorSSR file in the archive
    private static final String colorsFileName ="colors";

    // the collection of all colors SSR rules in the file
    private LinkedHashMap<String, Color> colorRules = new LinkedHashMap<String, Color>(10);

    public ColorsFile(ZipFile archive) {

        try {

            // open the colors file and set up the text scanner
            InputStream overlaysFile = archive.getInputStream(new ZipEntry(colorsFileName));
            Scanner scanner = new Scanner(overlaysFile).useDelimiter("\n");

            // read each line
            String nextLine = null;
            while (scanner.hasNext()){

                String line = scanner.next();

                // skip empty lines and comments (//)
                if(line.length() > 1 && !line.startsWith("//")){

                    String tokens[] = line.split("[, ]+");

                    String imageName = null;
                    int red = 0;
                    int green = 0;
                    int blue = 0;
                    try {
                        imageName = tokens[0].trim();
                        red = Integer.parseInt(tokens[1].trim());
                        green = Integer.parseInt(tokens[2].trim());
                        blue = Integer.parseInt(tokens[3].trim());
                    } catch (Exception e) {
                        System.out.println("The following line was ignored: " + line);
                    }

                    colorRules.put(imageName, new Color(red, green, blue));                }
            }

            // clean up
            scanner.close();
            overlaysFile.close();
        }
        catch (IOException e) {

            System.err.println("Error reading the overlaySSR file from the board archive");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Print the file as XML
     */
    public void printAsXML(){

        System.out.println("\t<colors>");
        for (String color: colorRules.keySet()) {

            System.out.println(
                    "\t\t<color name=\"" + color +
                            "\" red=\"" + colorRules.get(color).getRed() +
                            "\" green=\"" + colorRules.get(color).getGreen() +
                            "\" blue=\"" + colorRules.get(color).getBlue() +
                            "\" terrain=\"UNKNOWN" +
                            "\" elevation=\"UNKNOWN" +
                            "\" />"
            );
        }
        System.out.println("\t</colors>");
    }


    /**
     * Will extract the information in the archive color file and print the XML needed in the BoardMetadata.xml file
     * @param args
     */
    public static void main(String args[]) {

        if (args.length != 1) {
            System.out.println("Usage: java VASL.LOSGUI.ColorsFile <board archive>");
            System.exit(0);
        }
        try {

            ZipFile archive = new ZipFile(args[0]);
            ColorsFile file = new ColorsFile(archive);
            file.printAsXML();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
