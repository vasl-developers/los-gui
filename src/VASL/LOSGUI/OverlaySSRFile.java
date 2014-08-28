package VASL.LOSGUI;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Wraps the overlaySSR file in the board archive, loads the transformations into private classes, prints as XML
 */
public class OverlaySSRFile {

    // the name for colorSSR file in the archive
    private static final String overlaySSRFileName = "overlaySSR";

    // the collection of all overlay SSR rules in the file
    private LinkedHashMap<String, OverlaySSR> overlayRules = new LinkedHashMap<String, OverlaySSR>(10);
    private LinkedHashMap<String, UnderlaySSRule> underlayRules = new LinkedHashMap<String, UnderlaySSRule>(3);

    public OverlaySSRFile(ZipFile archive) {

        try {

            // open the overlay SSR file and set up the text scanner
            InputStream overlaysFile = archive.getInputStream(new ZipEntry(overlaySSRFileName));
            Scanner scanner = new Scanner(overlaysFile).useDelimiter("\n");

            // read each line
            String ruleName = null;
            String nextLine = null;
            OverlaySSR overlaySSR = null;
            while (scanner.hasNext()){

                String line = scanner.next();

                // skip empty lines and comments (//)
                if(line.length() <= 1 || line.startsWith("//")){

                }
                else {
                    // set up the new rule
                    ruleName = line.trim();
                    nextLine = scanner.next();

                    // line is an overlay
                    if(nextLine.startsWith("underlay")) {

                        String tokens[] = nextLine.split("[, ]+");

                        String imageName = tokens[1].trim();

                        ArrayList<String> colors = new ArrayList<String>(1);
                        for(int x = 2; x < tokens.length; x++) {

                            colors.add(tokens[x].trim());
                        }

                        underlayRules.put(ruleName, new UnderlaySSRule(ruleName, imageName, colors));
                    }

                    // line is a overlay
                    else  {
                        String imageName = null;
                        try{
                             imageName = nextLine.substring(0, nextLine.indexOf(" ")).trim();
                        } catch (Exception e){
                            System.err.println();
                        }
                        try {
                            nextLine = nextLine.substring(nextLine.indexOf(" "));
                            String x = nextLine.substring(0, nextLine.indexOf(",")).trim();
                            String y = nextLine.substring(nextLine.indexOf(",") + 1).trim();

                            overlayRules.put(ruleName, new OverlaySSR(ruleName, imageName, x, y));
                        } catch (Exception e) {
                            System.err.println("Invalid overlay: " + nextLine);
                        }
                    }
                }
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
     * A handy-dandy method for printing the file as XML
     */
    public void printAsXML(){

        System.out.println("<overlaySSRules>");
        for (String rule: underlayRules.keySet()) {

            underlayRules.get(rule).printAsXML();
        }

        for (String rule: overlayRules.keySet()) {

            overlayRules.get(rule).printAsXML();

        }
        System.out.println("</overlaySSRules>");
    }


	/**
	 * Will extract the information in the archive OverlaySSRFile and print the XML needed in
	 * the BoardMetadata.xml file
	 * @param args
	 */
	public static void main(String args[]) {

		if (args.length != 1) {
			System.out.println("Usage: java VASL.LOSGUI.OverlaySSRFile <board archive>");
			System.exit(0);
		}
        try {

            ZipFile archive = new ZipFile(args[0]);
            OverlaySSRFile file = new OverlaySSRFile(archive);
            file.printAsXML();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * A class for each SSR overlay transformation in the colorSSR file
     */
    private class OverlaySSR {

        // the color transformation rule name. E.g. AllPaved
        private String name;
        private String imageName;
        private String x;
        private String y;

        public OverlaySSR(String name, String imageName, String x, String y) {
            this.name = name;
            this.imageName = imageName;
            this.x = x;
            this.y = y;
        }

        void printAsXML() {

            System.out.println(
                    "\t<overlaySSRule name=\"" + name +
                            "\" image=\"" + imageName +
                            "\" x=\"" + x +
                            "\" y=\"" + y +
                            "\" />"
            );
        }

    }

    /**
     * A simple class for an SSR underlay transformation
     */
    public class UnderlaySSRule {

        private String name;
        private String imageName;
        private ArrayList<String> colors;

        UnderlaySSRule(String name, String imageName, ArrayList<String> colors) {

            this.name = name;
            this.imageName = imageName;
            this.colors = colors;
        }

        /**
         * @return the underlay SSR name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the underlay image name
         */
        public String getImageName() {
            return imageName;
        }

        /**
         * @return the list of colors
         */
        public ArrayList<String> getColors() {
            return colors;
        }

        public void printAsXML() {

            System.out.println(
                    "\t<underlaySSRule name=\"" + name +
                            "\" image=\"" + imageName +
                            "\">"
            );

            for(String color: colors) {

                System.out.println(
                        "\t\t<color name=\"" + color + "\" />"
                );

            }
            System.out.println("\t</underlaySSRule>");
        }
    }
}
