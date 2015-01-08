package VASL.LOS;

import VASL.LOS.Map.*;
import VASL.build.module.map.boardArchive.SharedBoardMetadata;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

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


/**
 * LOS data editing function used only by the LOS GUI
 */
public class GUILOSDataEditor extends LOSDataEditor {

    // standard elevation colors
    public final static Color VALLEY2_COLOR = new Color(88, 110, 50);
    public final static Color VALLEY1_COLOR = new Color(119, 146, 74);
    public final static Color LEVEL1_COLOR = new Color(176, 147, 70);
    public final static Color LEVEL2_COLOR = new Color(147, 111, 31);
    public final static Color LEVEL3_COLOR = new Color(118, 80, 0);
    public final static Color LEVEL4_COLOR = new Color(94, 57, 0);
    public final static Color LEVEL5_COLOR = new Color(74, 45, 0);
    public final static Color LEVEL6_COLOR = new Color(56, 34, 0);
    public final static Color LEVEL7_COLOR = new Color(60, 25, 0);
    public final static Color LEVEL8_COLOR = new Color(50, 20, 0);
    public final static Color LEVEL9_COLOR = new Color(40, 15, 0);
    public final static Color LEVEL10_COLOR = new Color(30, 10, 0);

    public GUILOSDataEditor(String boardName, String boardDirectory, SharedBoardMetadata sharedBoardMetadata) throws IOException {

        super(boardName, boardDirectory, sharedBoardMetadata);
    }

    /**
     * Repaints an area of the map image. If a bridge or prefab building touches the paint area,
     * the entire bridge/building is repainted as well. This should be the first paint routine
     * called when recreating an area of the map image.
     *
     * @param x         left-most pixel column
     * @param y         right-most pixel column
     * @param width     width of the paint area
     * @param height    height of the paint area
     * @param mapImage  the map image
     * @param imageList list of the terrain images to use
     */
    @SuppressWarnings("UnusedDeclaration")
    public void paintMapArea(int x, int y, int width, int height,
                             BufferedImage mapImage,
                             BufferedImage[] imageList,
                             Image singleHexWoodenBridgeImage,
                             Image singleHexStoneBridgeImage
    ) {

        Hex currentHex;
        Terrain depressionTerrain;
        int terrType;
        Rectangle paintArea = new Rectangle(x, y, width, height);


        // step through each pixel
        for (int i = Math.max(x, 0); i < Math.min(x + width + 1, map.getGridWidth() - 1); i++) {
            for (int j = Math.max(y, 0); j < Math.min(y + height + 1, map.getGridHeight() - 1); j++) {

                currentHex = map.gridToHex(i, j);

                terrType = map.getGridTerrain(i, j).getType();

                // get color for non-ground level open ground
                Color c = null;
                switch (map.getGridElevation(i, j)) {

                    case -4:
                        c = VALLEY2_COLOR;
                        break;
                    case -3:
                        c = VALLEY2_COLOR;
                        break;
                    case -2:
                        c = VALLEY2_COLOR;
                        break;
                    case -1:
                        c = VALLEY1_COLOR;
                        break;
                    case 1:
                        c = LEVEL1_COLOR;
                        break;
                    case 2:
                        c = LEVEL2_COLOR;
                        break;
                    case 3:
                        c = LEVEL3_COLOR;
                        break;
                    case 4:
                        c = LEVEL4_COLOR;
                        break;
                    case 5:
                        c = LEVEL5_COLOR;
                        break;
                    case 6:
                        c = LEVEL6_COLOR;
                        break;
                    case 7:
                        c = LEVEL7_COLOR;
                        break;
                    case 8:
                        c = LEVEL8_COLOR;
                        break;
                    case 9:
                        c = LEVEL9_COLOR;
                        break;
                    case 10:
                        c = LEVEL10_COLOR;
                        break;
                    case 11:
                        c = LEVEL10_COLOR;
                        break;
                    case 12:
                        c = LEVEL10_COLOR;
                        break;
                    case 13:
                        c = LEVEL10_COLOR;
                        break;
                    case 14:
                        c = LEVEL10_COLOR;
                        break;
                    case 15:
                        c = LEVEL10_COLOR;
                        break;
                }

                // image exist for this terrain?
                if (imageList[terrType] == null) {

                    // open ground color on an elevation?
                    if (map.getTerrain(terrType).getMapColor().equals(map.getTerrain("Open Ground").getMapColor())
                            && map.getGridElevation(i, j) != 0) {

                        mapImage.setRGB(i, j, c.getRGB());

                    } else {
                        mapImage.setRGB(i, j, map.getTerrain(terrType).getMapColor().getRGB());
                    }
                } else {

                    // open ground color on an elevation?
                    if (imageList[terrType].getRGB(i % imageList[terrType].getWidth(), j % imageList[terrType].getHeight()) ==
                            map.getTerrain("Open Ground").getMapColor().getRGB() &&
                            map.getGridElevation(i, j) != 0) {

                        mapImage.setRGB(i, j, c.getRGB());
                    } else {
                        mapImage.setRGB(i, j, imageList[terrType].getRGB(i % imageList[terrType].getWidth(), j % imageList[terrType].getHeight()));
                    }
                }
            }
        }
    }

    /**
     * Paints the contour lines into an area of the map image.
     * This should be called after paintMapArea when recreating the map image.
     *
     * @param x      left-most pixel column
     * @param y      right-most pixel column
     * @param width  width of the paint area
     * @param height height of the paint area
     * @param img    the map image
     */
    public void paintMapContours(int x, int y, int width, int height, BufferedImage img) {

        // create map in image
        int gridWidth = map.getGridWidth();
        int gridHeight = map.getGridHeight();
        for (int col = Math.max(x, 0); col < Math.min(x + width, gridWidth); col++) {
            for (int row = Math.max(y, 0); row < Math.min(y + height, gridHeight); row++) {

                // grid adjacent to lower ground level?
                if (((col > 0 && map.getGridElevation(col, row) > map.getGridElevation(col - 1, row)) ||
                        (row > 0 && map.getGridElevation(col, row) > map.getGridElevation(col, row - 1)) ||
                        (col < gridWidth - 1 && map.getGridElevation(col, row) > map.getGridElevation(col + 1, row)) ||
                        (row < gridHeight - 1 && map.getGridElevation(col, row) > map.getGridElevation(col, row + 1)))
                        ) {
                    img.setRGB(col, row, 0xFFFF0F0F);
                }
            }
        }
    }

    /**
     * Paints the shadows into an area of the map image.
     * This should be called after paintMapArea when recreating the map image.
     *
     * @param x      left-most pixel column
     * @param y      right-most pixel column
     * @param width  width of the paint area
     * @param height height of the paint area
     * @param img    the map image
     */
    public void paintMapShadows(int x, int y, int width, int height, BufferedImage img) {

        int currentHeight;
        int currentTerrainHeight;
        int groundLevel;
        Terrain currentTerrain;
        Hex currentHex = null;
        Hex tempHex;

        // Bridge stuff
        Bridge bridge = null;

        // number of pixels to shadow per level
        int pixelsPerLevel = 5;
        int pixelsPerHalfLevel = 2;

        // paint the map shadows in the image
        for (int col = Math.max(x, 0); col < Math.min(x + width - 1, map.getGridWidth()); col++) {

            // set the height of the first location in the grid column
            currentTerrain = map.getGridTerrain(col, Math.max(y - 1, 0));

            currentHeight = pixelsPerLevel * (currentTerrain.getHeight() + map.getGridElevation(col, Math.max(y - 1, 0)));

            // add half level height
            if (currentTerrain.isHalfLevelHeight()) {
                currentHeight += pixelsPerHalfLevel;

            }

            for (int row = Math.max(y - 1, 0); row < Math.min(y + height + pixelsPerLevel * 3, map.getGridHeight()); row++) {

                // set the current hex
                tempHex = map.gridToHex(col, row);
                if (tempHex != currentHex) {

                    // set the bridge
                    currentHex = tempHex;
                    bridge = currentHex.getBridge();
                }

                if (bridge != null && bridge.getShape().contains(col, row)) {

                    groundLevel = bridge.getRoadLevel() * pixelsPerLevel;
                    currentTerrainHeight = pixelsPerHalfLevel;

                } else {

                    currentTerrain = map.getGridTerrain(col, row);

                    currentTerrainHeight = currentTerrain.getHeight() * pixelsPerLevel;

                    if (currentTerrain.isHalfLevelHeight()) {

                        currentTerrainHeight += pixelsPerHalfLevel;
                    }
                    groundLevel = map.getGridElevation(col, row) * pixelsPerLevel;
                }

                // darken pixels in shadow
                if (currentTerrainHeight + groundLevel < currentHeight) {

                    // parse the pixel
                    int pixel = img.getRGB(col, row);
                    int alpha = pixel & 0xFF000000;
                    int red = pixel & 0x000000FF;
                    int green = (pixel & 0x0000FF00) >> 8;
                    int blue = (pixel & 0x00FF0000) >> 16;

                    // apply shadow
                    red = (int) ((float) red * 0.7);
                    green = (int) ((float) green * 0.7);
                    blue = (int) ((float) blue * 0.7);

                    // re-assemble and paint
                    pixel = alpha | red | (green << 8) | (blue << 16);
                    img.setRGB(col, row, pixel);

                    currentHeight -= 1;
                } else if (currentTerrainHeight + groundLevel > currentHeight) {

                    // parse the pixel
                    int pixel = img.getRGB(col, row);
                    int alpha = pixel & 0xFF000000;
                    int red = pixel & 0x000000FF;
                    int green = (pixel & 0x0000FF00) >> 8;
                    int blue = (pixel & 0x00FF0000) >> 16;

                    // apply highlight
                    red = (int) Math.min(255, (float) (red + 50));
                    green = (int) Math.min(255, (float) (green + 50));
                    blue = (int) Math.min(255, (float) (blue + 50));

                    // need to use custom color for woods
                    if (currentTerrain.getName().equals("Woods")) {
                        green = 250;
                    }
                    // re-assemble and paint
                    pixel = alpha | red | (green << 8) | (blue << 16);
                    img.setRGB(col, row, pixel);

                    // set the current height
                    currentHeight = currentTerrainHeight + groundLevel;
                }
            }
        }
    }

    /**
     * Paints hex terrain that is not in a location - e.g. slopes and railroads
     */
    public void paintAncillaryHexTerrain(BufferedImage img) {

        // get graphics handle
        Graphics2D workSpace = (Graphics2D) img.getGraphics();
        workSpace.setColor(Color.RED);
        workSpace.setFont(new Font("Arial", Font.BOLD, 12));

        Hex currentHex = null;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight() + (x % 2); y++) { // add 1 hex if odd
                currentHex = map.getHex(x, y);
                for (int z = 0; z < 6; z++) {
                    if (currentHex.hasSlope(z)) {

                        String s = "S";
                        workSpace.drawString(
                                s,
                                currentHex.getHexsideLocation(z).getEdgeCenterPoint().x - workSpace.getFontMetrics().stringWidth(s) / 2,
                                currentHex.getHexsideLocation(z).getEdgeCenterPoint().y
//                                        h.getCenterLocation().getLOSPoint().x -
//                                                workSpace.getFontMetrics().stringWidth(h.getName()) / 2 +
//                                                (h.getColumnNumber() == 0 ? 6 : 0) +
//                                                (h.getColumnNumber() == map.getHexGrid().length - 1 ? -7 : 0),
//                                        h.getHexsideLocation(0).getLOSPoint().y + 10
                        );
                    }
                }
            }
        }
    }

    /**
     * Paints the hex grid into the map image. Also paints the hex centers mark (including
     * tunnel/sewer, stairway symbols). Shows if smoke and entrenchments exist in hex (for now).
     * This should be called after all other map painting routines when recreating the map image.
     *
     * @param img the map image
     */
    // create hex outlines in image
    public void paintMapHexes(Image img) {

        paintMapHexes(img, Color.black);

    }

    // create hex outlines in image
    private void paintMapHexes(Image img, Color c) {

        // paint each hex
        Hex[][] hexGrid = map.getHexGrid();
        for (int col = 0; col < hexGrid.length; col++) {
            for (int row = 0; row < hexGrid[col].length; row++) {

                paintMapHex(img, hexGrid[col][row], true, c);
            }
        }
    }

    // paint a single hex
    private void paintMapHex(Image img, Hex h, boolean paintName, Color c) {

        // get graphics handle
        Graphics2D workSpace = (Graphics2D) img.getGraphics();
        workSpace.setColor(c);

        // draw hex border
        workSpace.drawPolygon(h.getHexBorder());

        // draw hex name
        if (paintName) {
            workSpace.setFont(new Font("Arial", Font.PLAIN, 10));

            workSpace.drawString(
                    h.getName(),
                    h.getCenterLocation().getLOSPoint().x -
                            workSpace.getFontMetrics().stringWidth(h.getName()) / 2 +
                            (h.getColumnNumber() == 0 ? 6 : 0) +
                            (h.getColumnNumber() == map.getHexGrid().length - 1 ? -7 : 0),
                    h.getHexsideLocation(0).getLOSPoint().y + 10
            );

            // draw hex center
            workSpace.setColor(Color.white);
            if (h.hasStairway() &&
                    !(h.getCenterLocation().getTerrain().getName().equals("Stone Building, 1 Level") ||
                            h.getCenterLocation().getTerrain().getName().equals("Wooden Building, 1 Level"))) {

                workSpace.fillRect(h.getHexCenter().x - 3, h.getHexCenter().y - 3, 6, 6);
            } else {
                workSpace.fillRect(h.getHexCenter().x - 1, h.getHexCenter().y - 1, 2, 2);
            }
        }
    }

    /**
     * Write the LOS data to the board archive
     */
    public void saveLOSData() {

        boardArchive.writeLOSData(map);
    }

    public String getArchiveName() {
        return boardArchive.getBoardName();
    }

    /**
     * Return a map of all terrain names and types
     *
     * @return
     */
    public HashMap<String, String> getTerrainNames() {

        HashMap<String, String> terrainNames = new HashMap<String, String>(boardArchive.getTerrainTypes().size());

        for (String key : sharedBoardMetadata.getTerrainTypes().keySet()) {

            terrainNames.put(key, boardArchive.getTerrainTypes().get(key).getLOSCategory().toString());
        }

        return terrainNames;

    }

    /**
     * @return the shared board metadata
     */
    public SharedBoardMetadata getSharedBoardMetadata() {
        return sharedBoardMetadata;
    }

    /**
     * Sets the hex ground level/depression terrain for a section of map.
     * Should be called after setGridGroundLevel.
     *
     * @param s        map area to update
     * @param terr     applicable depression terrain
     * @param newLevel new ground level
     */
    public void setHexGroundLevel(Shape s, Terrain terr, int newLevel) {

        // set the hex base elevation and depression terrain
        Vector v = intersectedHexes(map, s.getBounds());
        Iterator iter = v.iterator();
        Hex currentHex = null;
        Location center = null;
        while (iter.hasNext()) {

            currentHex = (Hex) iter.next();
            center = currentHex.getCenterLocation();

            // set the center location
            if (s.contains(center.getLOSPoint())) {

                if (terr != null) {

                    // hex being set to depression terrain?
                    if (!center.isDepressionTerrain()) {

                        currentHex.setBaseHeight(map.getGridElevation(
                                (int) center.getLOSPoint().getX(),
                                (int) center.getLOSPoint().getY()));

                        //set the depression terrain, base level of the six hexsides
                        for (int x = 0; x < 6; x++) {

                            // on map?
                            if ((x == 0 && currentHex.isNorthOnMap()) ||
                                    (x == 1 && currentHex.isNorthEastOnMap()) ||
                                    (x == 2 && currentHex.isSouthEastOnMap()) ||
                                    (x == 3 && currentHex.isSouthOnMap()) ||
                                    (x == 4 && currentHex.isSouthWestOnMap()) ||
                                    (x == 5 && currentHex.isNorthWestOnMap())

                                    ) {

                                // if the hexside location has the same elevation as the center
                                // (on the grid), then make it a depression location
                                if (map.getGridElevation(
                                        (int) center.getLOSPoint().getX(),
                                        (int) center.getLOSPoint().getY())
                                        ==
                                        map.getGridElevation(
                                                (int) currentHex.getHexsideLocation(x).getEdgeCenterPoint().getX(),
                                                (int) currentHex.getHexsideLocation(x).getEdgeCenterPoint().getY())
                                        ) {
                                    currentHex.getHexsideLocation(x).setBaseHeight(0);
                                    currentHex.getHexsideLocation(x).setDepressionTerrain(terr);
                                } else {
                                    // non-depression hexside locations are one level higher
                                    currentHex.getHexsideLocation(x).setBaseHeight(1);
                                }
                            }
                        }
                    }
                } else {
                    currentHex.setBaseHeight(newLevel);
                }

                // update the depression terrain for the hex
                currentHex.setDepressionTerrain(terr);
            }

            // set the depression terrain on the hexsides
            if (terr != null) {
                for (int x = 0; x < 6; x++) {
                    if (s.contains(currentHex.getHexsideLocation(x).getEdgeCenterPoint())) {

                        currentHex.setHexsideDepressionTerrain(x);

                        // if center is depression, ensure base level is reset
                        if (center.isDepressionTerrain()) {
                            currentHex.getHexsideLocation(x).setBaseHeight(0);
                        }
                    }
                }
            }
        }
    }

    /**
     * Maps all terrain from one type to another in the whole map.
     *
     * @parameter fromTerrain original terrain to replace
     * @parameter toTerrain new terrain type
     */
    public boolean changeAllTerrain(Terrain fromTerrain, Terrain toTerrain) {

        return changeAllTerrain(fromTerrain, toTerrain, new Rectangle(0, 0, map.getGridWidth(), map.getGridHeight()));
    }

    /**
     * Maps all ground level elevation from one level to another in the whole map.
     *
     * @parameter fromElevation original ground level elevation to replace
     * @parameter toTerrain new ground level elevation
     */
    public boolean changeAllGroundLevel(int fromElevation, int toElevation) {

        return changeAllGroundLevel(fromElevation, toElevation, new Rectangle(0, 0, map.getGridWidth(), map.getGridHeight()));
    }

    /**
     * This method is intended to be used only to copy geomorphic maps into
     * a larger map "grid" for VASL. As such, 1) it is assumed the half hex along board
     * edges are compatible, and 2) the hex/location names from the map that is being
     * inserted should be used. Other uses will produce unexpected results.
     */
    public boolean insertMap(Map insertMap, Hex upperLeft) {

        return map.insertMap(insertMap, upperLeft);
    }

    /**
     * Sets the hex terrain for an area of the map. All locations within the given shape are changed.
     *
     * @param s    map area to change
     * @param terr new terrain type
     */
    public void setHexTerrain(Shape s, Terrain terr) {

        // get the affected hexes
        Vector<Hex> v = intersectedHexes(map, s.getBounds());
        Hex currentHex = null;
        for (Hex aHex : v) {

            // set the center location
            if (s.contains(aHex.getCenterLocation().getLOSPoint())) {

                aHex.getCenterLocation().setTerrain(terr);

            }

            // set the terrain on the hexsides
            for (int x = 0; x < 6; x++) {
                if (s.contains(aHex.getHexsideLocation(x).getEdgeCenterPoint())) {

                    aHex.setHexsideLocationTerrain(x, terr);
                }
            }
        }
    }

    /**
     * Sets all pixels within the given rectangle to the new terrain type.
     *
     * @param rect map area to update
     * @param terr new terrain type
     */
    public void setGridTerrain(Rectangle rect, Terrain terr) {

        int startX = (int) rect.getX();
        int startY = (int) rect.getY();

        // set the terrain in the map grid
        for (int x = Math.max(startX, 0);
             x < Math.min(startX + rect.getWidth(), map.getGridWidth());
             x++) {
            for (int y = Math.max(startY, 0);
                 y < Math.min(startY + rect.getHeight(), map.getGridHeight());
                 y++) {

                map.setGridTerrainCode(terr.getType(), x, y);

            }
        }

        // set the factory walls, if necessary
        if (terr.isFactoryTerrain()) {
            setFactoryWalls(rect, terr);
        }
    }
}
