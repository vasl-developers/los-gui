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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JComponent;
import javax.swing.Scrollable;

import VASL.LOS.GUILOSDataEditor;
import VASL.LOS.Map.Bridge;
import VASL.LOS.Map.Hex;
import VASL.LOS.Map.LOSResult;
import VASL.LOS.Map.Location;
import VASL.LOS.Map.Map;
import VASL.LOS.Map.Terrain;
import VASL.LOS.VASLGameInterface;
import VASL.LOSGUI.Selection.BridgeSelection;
import VASL.LOSGUI.Selection.HexSelection;
import VASL.LOSGUI.Selection.HexsideSelection;
import VASL.LOSGUI.Selection.RectangularSelection;
import VASL.LOSGUI.Selection.RotatedRectangularSelection;
import VASL.LOSGUI.Selection.Selection;
import VASL.build.module.ASLMap;
import VASL.build.module.map.boardArchive.BoardArchive;
import VASL.build.module.map.boardArchive.SharedBoardMetadata;
import VASSAL.tools.DataArchive;

import static VASSAL.build.GameModule.getGameModule;

/**
 * Title:        LOSEditorJComponent.java
 * Copyright:    Copyright (c) 2001 David Sullivan Zuericher Strasse 6 12205 Berlin Germany. All rights reserved.
 *
 * @author David Sullivan
 * @version 1.0
 */
public class LOSEditorJComponent
        extends JComponent
        implements MouseListener,
        MouseMotionListener,
        Scrollable,
        KeyListener {

    // status variables
    private boolean mapChanged;
    private boolean mapOpen;

    private static final String sharedBoardMetadataFileName = "SharedBoardMetadata.xml"; // name of the shared board metadata file

    // the map editor
    public GUILOSDataEditor losDataEditor;
    public BufferedImage mapImage;
    private int minDirtyX = -1;
    private int minDirtyY = -1;
    private int maxDirtyX = -1;
    private int maxDirtyY = -1;
    private BufferedImage terrainImages[] = new BufferedImage[256];
    private Image singleHexWoodenBridgeImage;
    private Image singleHexStoneBridgeImage;

    // function variables
    private String currentFunctionName = "LOS";
    private String currentTerrainName;
    private Terrain currentTerrain;
    private String currentToTerrainName;
    private Terrain currentToTerrain;
    private String currentBrush;
    private int currentBrushSize;
    private int currentGroundLevel;
    private int currentToGroundLevel;
    private boolean roundBrush;

    private boolean VASLImage;
    private int rotation;

    // pseudo mouse cursors
    private Shape cursorShape;

    // custom building variables
    private int customBuildingWidth = 32;
    private int customBuildingHeight = 32;
    private boolean customBridgeOn;

    private static final int EDGE_TERRAIN_WIDTH = 39;
    private static final int EDGE_TERRAIN_HEIGHT = 5;

    // custom bridge variables
    private Bridge currentBridge;
    private int customBridgeRoadElevation;

	private int roadWidth;
	private int roadHeight;

    // selection list
    private LinkedList<Selection> allSelections = new LinkedList<Selection>();

    // ZIP file archive stuff
    private ZipFile archive;
    private SharedBoardMetadata sharedBoardMetadata;
    private VASLGameInterface vaslGameInterface;
    private VASL.LOS.Map.Map LOSMap;

    protected DataArchive dataArchive;

    private boolean doingLOS;
    private int targetX;
    private int targetY;
    private int sourceX;
    private int sourceY;

    private LOSResult result = new LOSResult();
    private Location sourceLocation;
    private Location targetLocation;

    private boolean useAuxSourceLOSPoint;

    private LOSEditorJFrame frame;

    private Dimension dim;

    public LOSEditorJComponent() {

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFrame(LOSEditorJFrame newFrame) {

        frame = newFrame;
    }

    //Component initialization
    private void jbInit() throws Exception {

		setMinimumSize(new Dimension(100, 100));
		setEnabled(true);
        adjustMapViewSize();
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        // set up the image archive
        final String archiveName = LOSEditorProperties.getLOSEditorHome() + System.getProperty("file.separator", "\\") + "LOSEditorData.zip";
        // archiveName = "\"" + archiveName + "\"";
        try {

            archive = new ZipFile(archiveName);

        } catch (IOException e) {

            System.err.println("Cannot read the archive file " + archiveName);
            System.err.println("Exiting...");
            System.exit(1);
        }

        // read the shared metadata file
        loadSharedBoardMetadata();
    }

    /**
     * Reads the metadata from the SharedBoardMetadata.xml file in the VASL version being used
     */
    public void loadSharedBoardMetadata() throws IOException {

        sharedBoardMetadata = new SharedBoardMetadata();
        try {
            // read the shared metadata file in the VASL dist folder and set the terrain types
            String sBMdFN = LOSEditorProperties.getShardBoardMetadataFileName();
            InputStream inputStream = new FileInputStream(sBMdFN);
            sharedBoardMetadata.parseSharedBoardMetadataFile(inputStream);
        } catch (Exception e) {

            throw new IOException("Unable to read the shared board metadata from the LOS archive",e);
        }
    }

    // load the terrain graphics
    public void loadTerrainGraphics() {

        final String[] sbuf = new String[256];
        final String fileName = "CASL/init/TerrainImages.txt";

        String s;
        int current = 0;
        Graphics g;
        Image tempImage;

        // open and read the file
        try {

            final BufferedReader r = new BufferedReader(new InputStreamReader(getTextFile(fileName)));

            // read in the text line for each building
            while ((s = r.readLine()) != null && current < 256) {

                sbuf[current++] = s;
            }
            r.close();
        } catch (IOException e) {

            // handle error
            LOSEditorApp.writeError("Error reading the terrain images file " + fileName);
        }

        // get the images
        current = 0;
        while (sbuf[current] != null && current < 256) {

            String terrainName = "";
            String imageFileName = "";

            // get the terrain and image names
            try {
                terrainName = sbuf[current].substring(0, sbuf[current].indexOf('|'));
                imageFileName = sbuf[current].substring(sbuf[current].indexOf('|') + 1);
            } catch (Exception e) {

                LOSEditorApp.writeError("Line " + (current + 1) + ": Cannot read the line... " + sbuf[current]);
            }

            // find the terrain
            final Terrain t = losDataEditor.getMap().getTerrain(terrainName);
            if (t == null) {

                LOSEditorApp.writeError("Line " + (current + 1) + ": Terrain not found - " + terrainName);
            }

            // load the graphic
            else {

                // get the image from the file
                try {

                    tempImage = getImage("CASL/images/terrain/" + imageFileName);

                    // no buffered image for bridges (allow for transparency)
                    if (t.getType() == losDataEditor.getMap().getTerrain("Single Hex Wooden Bridge").getType()) {

                        singleHexWoodenBridgeImage = tempImage;
                    } else if (t.getType() == losDataEditor.getMap().getTerrain("Single Hex Stone Bridge").getType()) {

                        singleHexStoneBridgeImage = tempImage;
                    } else {

                        // draw it into the buffered image
                        terrainImages[t.getType()] = new BufferedImage(tempImage.getWidth(this), tempImage.getHeight(this), BufferedImage.TYPE_3BYTE_BGR);
                        g = terrainImages[t.getType()].getGraphics();
                        g.drawImage(tempImage, 0, 0, this);

                        // free up resources
                        g.dispose();
                    }

                } catch (Exception e) {
                    terrainImages[t.getType()] = null;
                    LOSEditorApp.writeError("Line " + (current + 1) + ": Cannot find terrain image file " + imageFileName);
                }
            }

            current++;
        }
    }

    public String getArchiveName() {
        return losDataEditor.getArchiveName();
    }

    public boolean isMapOpen() {
        return mapOpen;
    }

    public boolean isMapChanged() {
        return mapChanged;
    }

    public void adjustMapViewSize() {

        // adjust window to map size
        if (losDataEditor == null) {

			setPreferredSize(new Dimension(0, 0));
			revalidate();
        } else {
            dim = new Dimension(losDataEditor.getMap().getGridWidth(), losDataEditor.getMap().getGridHeight());
			setPreferredSize(dim);
			revalidate();
        }
    }

    public void paint(Graphics g) {

        // is the map open?
        if (!mapOpen) return;

        // paint the map
        final Graphics2D screen2D = (Graphics2D) g;
        if (VASLImage) {
            //losDataEditor.paintMapShadows(0,0,i.getWidth(), i.getHeight(), i);
            BufferedImage i = new BufferedImage(losDataEditor.getMap().getGridWidth(), losDataEditor.getMap().getGridHeight(), BufferedImage.TYPE_3BYTE_BGR);
            i.getGraphics().drawImage(losDataEditor.getBoardImage(), 0, 0, null);
//            losDataEditor.paintMapShadows(0,0,i.getWidth(),i.getHeight(),i);
            screen2D.drawImage(i, 0, 0, this);

        }
        else {
            screen2D.drawImage(mapImage, 0, 0, this);
        }

        // paint the scenario units
        screen2D.setColor(Color.white);

        if ("LOS".equals(currentFunctionName)) {

            final int spacing = 20;

            // set level color
            switch (sourceLocation.getBaseHeight() + sourceLocation.getHex().getBaseHeight()) {

                case -1:
                case -2:
                    screen2D.setColor(Color.red);
                    break;
                case 0:
                    screen2D.setColor(Color.gray);
                    break;
                case 1:
                    screen2D.setColor(Color.darkGray);
                    break;
                case 2:
                    screen2D.setColor(Color.black);
                    break;
                default:
                    screen2D.setColor(Color.white);
            }

            // draw the source location level
            screen2D.drawString(
                    "Level " + (sourceLocation.getBaseHeight() + sourceLocation.getHex().getBaseHeight()),
                    (int) sourceLocation.getLOSPoint().getX() - spacing / 2,
                    (int) sourceLocation.getLOSPoint().getY() + spacing / 2 + 15
            );


            if (doingLOS) {

                if (result.isBlocked()) {
                    if (result.hasHindrance()) {

                        screen2D.setColor(Color.white);
                        screen2D.drawLine(
                                sourceX,
                                sourceY,
                                (int) result.firstHindranceAt().getX(),
                                (int) result.firstHindranceAt().getY());

                        screen2D.setColor(Color.red);
                        screen2D.drawLine(
                                (int) result.firstHindranceAt().getX(),
                                (int) result.firstHindranceAt().getY(),
                                (int) result.getBlockedAtPoint().getX(),
                                (int) result.getBlockedAtPoint().getY());

                        screen2D.setColor(Color.black);
                        screen2D.drawLine(
                                (int) result.getBlockedAtPoint().getX(),
                                (int) result.getBlockedAtPoint().getY(),
                                targetX,
                                targetY);
                    } else {
                        screen2D.setColor(Color.white);
                        screen2D.drawLine(
                                sourceX,
                                sourceY,
                                (int) result.getBlockedAtPoint().getX(),
                                (int) result.getBlockedAtPoint().getY());

                        screen2D.setColor(Color.black);
                        screen2D.drawLine(
                                (int) result.getBlockedAtPoint().getX(),
                                (int) result.getBlockedAtPoint().getY(),
                                targetX,
                                targetY);
                    }
                } else if (result.hasHindrance()) {

                    screen2D.setColor(Color.white);
                    screen2D.drawLine(
                            sourceX,
                            sourceY,
                            (int) result.firstHindranceAt().getX(),
                            (int) result.firstHindranceAt().getY());

                    screen2D.setColor(Color.red);
                    screen2D.drawLine(
                            (int) result.firstHindranceAt().getX(),
                            (int) result.firstHindranceAt().getY(),
                            targetX,
                            targetY);
                } else {

                    screen2D.setColor(Color.white);
                    screen2D.drawLine(
                            sourceX,
                            sourceY,
                            targetX,
                            targetY);
                }

                // draw the target location level
                screen2D.setColor(Color.red);
                screen2D.drawString(
                        "Level " + (targetLocation.getBaseHeight() + targetLocation.getHex().getBaseHeight()),
                        (int) targetLocation.getLOSPoint().getX() - spacing / 2,
                        (int) targetLocation.getLOSPoint().getY() + spacing / 2 + 15
                );
            }
        } else {
            for (Object allSelection : allSelections) {

                ((Selection) allSelection).paint(screen2D);
            }
        }

        // show mouse pseudo cursor
        if (cursorShape != null) {

            // red square
            screen2D.setColor(Color.white);
            screen2D.draw(cursorShape);
        }

        // free resources
        screen2D.dispose();
    }

    public void update(Graphics screen) {
        paint(screen);
    }

    /**
     * ***************************
     * Mouse methods
     * ****************************
     */
    public void mouseReleased(MouseEvent e) {

        // is the map open?
        if (!mapOpen) return;

        final Map map = losDataEditor.getMap();

        if (map.onMap(e.getX(), e.getY())) {
            if ("LOS".equals(currentFunctionName)) {

                return;

            }
            if ("Set ground level".equals(currentFunctionName) ||
				"Add terrain".equals(currentFunctionName)) {

                // custom building?
                if (customBridgeOn) {

                    // create, rotate and add the rectangle
                    final AffineTransform at = AffineTransform.getRotateInstance(
                            Math.toRadians((double)rotation),
						(double)e.getX(),
						(double)e.getY()
                    );
                    allSelections.add(new RectangularSelection(
                            at.createTransformedShape(new Rectangle(
                                    e.getX() - customBuildingWidth / 2,
                                    e.getY() - customBuildingHeight / 2,
                                    customBuildingWidth,
                                    customBuildingHeight)),
                            false
                    ));
                }

                // full hex selected?
                else if ("Hex".equals(currentBrush)) {

                    final Hex h = losDataEditor.getMap().gridToHex(e.getX(), e.getY());

                    // mark the hex
                    allSelections.add(new HexSelection(h));

                } else {
                    final int currentX = e.getX() - currentBrushSize / 2;
                    final int currentY = e.getY() - currentBrushSize / 2;

                    // need to rotate?
                    if (rotation != 0 && !roundBrush) {

                        // create, rotate and add the rectangle
                        final AffineTransform at = AffineTransform.getRotateInstance(
                                Math.toRadians((double)rotation),
							(double)e.getX(),
							(double)e.getY()
                        );
                        allSelections.add(new RotatedRectangularSelection(
                                at.createTransformedShape(new Rectangle(
                                        e.getX() - currentBrushSize / 2,
                                        e.getY() - currentBrushSize / 2,
                                        currentBrushSize,
                                        currentBrushSize)),
                                e.getX() - currentBrushSize / 2,
                                e.getY() - currentBrushSize / 2,
                                currentBrushSize,
                                currentBrushSize,
                                rotation
                        ));
                    } else {

                        // create the rectangle and add
                        final Rectangle rect = new Rectangle(currentX, currentY, currentBrushSize, currentBrushSize);
                        allSelections.add(new RectangularSelection(rect, roundBrush));
                    }
                }
            }
            else if ("Add hexside terrain".equals(currentFunctionName)) {

                final Location sourceLocation = losDataEditor.getMap().gridToHex(e.getX(), e.getY()).getNearestLocation(e.getX(), e.getY());
                final Hex hex = sourceLocation.getHex();

                //ignore the center location
                if (!hex.isCenterLocation(sourceLocation)) {

                    // create a hexside rectangles
                    final Rectangle paintRect = new Rectangle(
                            (int) sourceLocation.getEdgeCenterPoint().getX() - EDGE_TERRAIN_WIDTH / 2,
                            (int) sourceLocation.getEdgeCenterPoint().getY() - EDGE_TERRAIN_HEIGHT / 2,
                            EDGE_TERRAIN_WIDTH,
                            EDGE_TERRAIN_HEIGHT
                    );
                    final Rectangle gridRect = new Rectangle(
                            (int) sourceLocation.getEdgeCenterPoint().getX() - EDGE_TERRAIN_WIDTH / 2 - 1,
                            (int) sourceLocation.getEdgeCenterPoint().getY() - EDGE_TERRAIN_HEIGHT / 2 - 1,
                            EDGE_TERRAIN_WIDTH + 1,
                            EDGE_TERRAIN_HEIGHT + 1
                    );

                    // need to rotate?
                    int degrees = 0;
                    final int side = hex.getLocationHexside(sourceLocation);
                    switch (side) {
                        case 1:
                        case 4:
                            degrees = 60;
                            break;
                        case 2:
                        case 5:
                            degrees = -60;
                            break;
                    }

                    // rotate the rectangle
                    final AffineTransform at = AffineTransform.getRotateInstance(
                            Math.toRadians((double)degrees),
                            sourceLocation.getEdgeCenterPoint().getX(),
                            sourceLocation.getEdgeCenterPoint().getY()
                    );
                    allSelections.add(new HexsideSelection(
                            at.createTransformedShape(paintRect),
                            at.createTransformedShape(gridRect),
                            sourceLocation
                    ));
                }
            }
            else if ("Add bridge".equals(currentFunctionName)) {

                final Hex h = map.gridToHex(e.getX(), e.getY());
                // remove?
                if (currentTerrain == null) {

                    allSelections.add(new HexSelection(h));
                } else {
                    allSelections.add(new BridgeSelection(new Bridge(
                            currentBridge.getTerrain(),
						customBridgeRoadElevation,
                            currentBridge.getRotation(),
                            currentBridge.getLocation(),
                            currentBridge.isSingleHex(),
                            currentBridge.getCenter()
                    )));
                }
            }
            else if ("Add road".equals(currentFunctionName)) {

                final Location sourceLocation = map.gridToHex(e.getX(), e.getY()).getNearestLocation(e.getX(), e.getY());
                final Hex hex = sourceLocation.getHex();

                // only place elevated roads on level 0
                if (!"Elevated Road".equals(hex.getCenterLocation().getTerrain().getName()) &&
					"Elevated Road".equals(currentTerrain.getName()) &&
                        hex.getBaseHeight() != 0)
                    return;

                //ignore the center location
                if (!hex.isCenterLocation(sourceLocation)) {

                    // create the road rectangle
                    final int roadOffset = 4;
                    final Rectangle roadRect = new Rectangle(
                            (int) hex.getCenterLocation().getLOSPoint().getX() - roadWidth / 2,
                            (int) hex.getCenterLocation().getLOSPoint().getY() - roadHeight - 1,
                            roadWidth,
                            roadHeight + roadOffset
                    );

                    final Rectangle elevationRect = new Rectangle(
                            (int) hex.getCenterLocation().getLOSPoint().getX() - roadWidth / 2 - 4,
                            (int) hex.getCenterLocation().getLOSPoint().getY() - roadHeight - 1,
                            roadWidth + 8,
                            roadHeight + roadOffset
                    );

                    // need to rotate?
                    int degrees = 0;
                    final int side = hex.getLocationHexside(sourceLocation);
                    switch (side) {
                        case 1:
                            degrees = 60;
                            break;
                        case 2:
                            degrees = 120;
                            break;
                        case 3:
                            degrees = 180;
                            break;
                        case 4:
                            degrees = -120;
                            break;
                        case 5:
                            degrees = -60;
                            break;
                    }

                    // rotate the rectangle
                    final AffineTransform at = AffineTransform.getRotateInstance(
                            Math.toRadians((double)degrees),
                            hex.getCenterLocation().getLOSPoint().getX(),
                            hex.getCenterLocation().getLOSPoint().getY()
                    );
                    allSelections.add(new HexsideSelection(
                            at.createTransformedShape(roadRect),
                            at.createTransformedShape(elevationRect),
                            sourceLocation
                    ));
                }
            }
            else if("Add objects".equals(currentFunctionName)){

                // mark the hex
                final Hex h = map.gridToHex(e.getX(), e.getY());
                allSelections.add(new HexSelection(h));
            }

            repaint();
        }
    }

    public void mousePressed(MouseEvent e) {

        if ("LOS".equals(currentFunctionName)) {

            sourceLocation = losDataEditor.getMap().gridToHex(e.getX(), e.getY()).getNearestLocation(e.getX(), e.getY());
            sourceX = sourceLocation.getLOSPoint().x;
            sourceY = sourceLocation.getLOSPoint().y;

            // if Ctrl click, use upper-most location
            if (e.isControlDown()) {
                while (sourceLocation.getUpLocation() != null) {
                    sourceLocation = sourceLocation.getUpLocation();
                }
            }
            doingLOS = true;
            mouseDragged(e);

        }
        requestFocus();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {

        if(losDataEditor == null) return;

        final int mouseX = e.getX();
        final int mouseY = e.getY();

        final Map map = losDataEditor.getMap();

        // is the map open?
        if (!mapOpen) return;
        if (!map.onMap(mouseX, mouseY)) return;

        final Location newLocation = map.gridToHex(mouseX, mouseY).getNearestLocation(mouseX, mouseY);

        // set the pseudo mouse cursor
        if ("Set ground level".equals(currentFunctionName) ||
			"Add terrain".equals(currentFunctionName)) {

            if (customBridgeOn) {

                // create and rotate the rectangle
                final AffineTransform at = AffineTransform.getRotateInstance(
                        Math.toRadians((double)rotation),
					(double)e.getX(),
					(double)e.getY()
                );
                cursorShape = at.createTransformedShape(new Rectangle(
                        e.getX() - customBuildingWidth / 2,
                        e.getY() - customBuildingHeight / 2,
                        customBuildingWidth,
                        customBuildingHeight));

            } else if (roundBrush) {

                // set the cursor rectangle
                cursorShape = new Ellipse2D.Float(
                        (float) mouseX - (float)(currentBrushSize / 2),
                        (float) mouseY - (float)(currentBrushSize / 2),
                        (float) currentBrushSize,
                        (float) currentBrushSize
                );

            } else {
                // create and rotate the rectangle
                final AffineTransform at = AffineTransform.getRotateInstance(
                        Math.toRadians((double)rotation),
					(double)e.getX(),
					(double)e.getY()
                );

                // set the cursor rectangle
                cursorShape = at.createTransformedShape(new Rectangle(
                        (mouseX - currentBrushSize / 2),
                        (mouseY - currentBrushSize / 2),
                        currentBrushSize,
                        currentBrushSize
                ));
            }
            repaint();
        }
        else if ("Add bridge".equals(currentFunctionName)) {

            if (currentBridge != null) {

                currentBridge.setCenter(e.getPoint());
                currentBridge.setRotation(rotation);
                currentBridge.setLocation(map.gridToHex(e.getX(), e.getY()).getCenterLocation());

                cursorShape = currentBridge.getShape();
                repaint();
            }
        }
        else {
            cursorShape = null;
        }

        targetLocation = newLocation;
        final String cursorString =
                " X: " + mouseX +
                        " Y: " + mouseY +
                        " Z: " + (map.getGridElevation(mouseX, mouseY) +
                        map.getGridTerrain(mouseX, mouseY).getHeight()) +
                        " (" + map.getGridTerrain(mouseX, mouseY).getName() +
                        ")";

        final String locationString = " | Location: " + " " + targetLocation.getName();

        final String heightString =
                " - Height: " + (targetLocation.getHex().getBaseHeight() + targetLocation.getBaseHeight());

        String terrainString = " - Terrain:  " + targetLocation.getTerrain().getName();

        // depression terrain?
        if (targetLocation.getDepressionTerrain() != null) {
            terrainString += "/" + targetLocation.getDepressionTerrain().getName();
        }

        // edge/cliff terrain?
        if (!targetLocation.getHex().isCenterLocation(targetLocation)) {
            if (targetLocation.getHex().getHexsideTerrain(targetLocation.getHex().getLocationHexside(targetLocation)) != null) {
                terrainString += "/" + targetLocation.getHex().getHexsideTerrain(targetLocation.getHex().getLocationHexside(targetLocation)).getName();
            }
            if (targetLocation.getHex().hasCliff(targetLocation.getHex().getLocationHexside(targetLocation))) {
                terrainString += "/Cliff";
            }
        }

        //Bridge?
        if (targetLocation.getHex().hasBridge()) {
            terrainString += "/" + targetLocation.getHex().getBridge().getTerrain().getName();
        }

        //stairway?
        terrainString += " | Stairway: " + targetLocation.getHex().hasStairway();

        frame.setStatusBarText(cursorString + locationString + heightString + terrainString);
    }

    public void mouseDragged(MouseEvent e) {

        final int mouseX = e.getX();
        final int mouseY = e.getY();

        // is the map open?
        if (!mapOpen) return;
        final Map map = losDataEditor.getMap();

        if (!map.onMap(mouseX, mouseY)) return;

        final Location newLocation = map.gridToHex(mouseX, mouseY).getNearestLocation(mouseX, mouseY);

        if ("LOS".equals(currentFunctionName)) {

            if (doingLOS) {

                final boolean useAuxTargetLOSPoint = newLocation.auxLOSPointIsCloser(mouseX, mouseY);
                final Point LOSPoint = useAuxTargetLOSPoint ? newLocation.getAuxLOSPoint() : newLocation.getLOSPoint();

                // are we really in a new location?
                if (targetLocation == newLocation && targetX == (int) LOSPoint.getX() && targetY == (int) LOSPoint.getY()) {
                    return;
                }

                targetLocation = newLocation;

                // if Ctrl click, use upper location
                if (e.isControlDown()) {
                    while (targetLocation.getUpLocation() != null) {
                        targetLocation = targetLocation.getUpLocation();
                    }
                }

                targetX = (int) LOSPoint.getX();
                targetY = (int) LOSPoint.getY();

                vaslGameInterface = new VASLGameInterface(null, null);
                map.LOS(sourceLocation, useAuxSourceLOSPoint, targetLocation, useAuxTargetLOSPoint, result, vaslGameInterface);

                if (result.isBlocked()) {
                    frame.setStatusBarText(
                            "Blocked at " + (int) result.getBlockedAtPoint().getX() + ", "
                                    + (int) result.getBlockedAtPoint().getY() +
                                    " Reason: " + result.getReason() +
                                    " Horizontal? " + result.isLOSisHorizontal() +
                                    " 60 deg? " + result.isLOSis60Degree()
                    );
                } else {
                    frame.setStatusBarText(
                            " Hindrances: " + result.getHindrance() +
                                    " Continuous slope: " + result.isContinuousSlope() +
                                    " Range: " + result.getRange() +
                                    " | Horizontal? " + result.isLOSisHorizontal() +
                                    " | 60 deg? " + result.isLOSis60Degree()
                    );
                }

                repaint();
            }
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        return dim;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (losDataEditor == null) {
            return 0;
        } else {
            return (int) losDataEditor.getMap().getHeight();
        }
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (losDataEditor == null) {
            return 0;
        } else {
            return 200;
        }
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void setCurrentFunction(String newCurrentFunction) {

        if(losDataEditor == null) {
            return;
        }

        clearSelections();
        frame.setStatusBarText("  ");

        final Map map = losDataEditor.getMap();

        if ("LOS".equals(newCurrentFunction)) {
            if (!"LOS".equals(currentFunctionName)) {

                requestFocus();
                doingLOS = false;
                currentFunctionName = newCurrentFunction;
            }
        } else if ("Set ground level".equals(newCurrentFunction)) {
            if (!"Set ground level".equals(currentFunctionName)) {

                currentFunctionName = newCurrentFunction;
                currentGroundLevel = 0;
                currentToGroundLevel = 0;
            }
        } else if ("Add hexside terrain".equals(newCurrentFunction)) {
            if (!"Add hexside terrain".equals(currentFunctionName)) {
                currentFunctionName = newCurrentFunction;

                // start with wall terrain
                currentTerrain = map.getTerrain("Wall");
                currentTerrainName = "Wall";
                currentToTerrain = map.getTerrain("Wall");
                currentToTerrainName = "Wall";
            }
        } else if ("Add terrain".equals(newCurrentFunction)) {
            if (!"Add terrain".equals(currentFunctionName)) {
                currentFunctionName = newCurrentFunction;

                // start with open ground
                currentTerrain = map.getTerrain("Open Ground");
                currentTerrainName = "Open ground";
                currentToTerrain = map.getTerrain("Open Ground");
                currentToTerrainName = "Open ground";
                customBridgeOn = false;
            }
        } else if ("Add bridge".equals(newCurrentFunction)) {
            if (!"Add bridge".equals(currentFunctionName)) {
                currentFunctionName = newCurrentFunction;
                currentTerrain = map.getTerrain("Single Hex Wooden Bridge");
                currentTerrainName = "Wooden Building, One level";
            }
        } else if ("Add road".equals(newCurrentFunction)) {
            if (!"Add road".equals(currentFunctionName)) {
                currentFunctionName = newCurrentFunction;
                currentTerrain = map.getTerrain("Dirt Road");
                currentTerrainName = "Dirt Road";
                currentToTerrain = map.getTerrain("Dirt Road");
                currentToTerrainName = "Dirt Road";
            }
        }
        else if ("Add objects".equals(newCurrentFunction)) {
            if (!"Add objects".equals(currentFunctionName)) {
                currentFunctionName 	= newCurrentFunction;
                currentTerrain 		= map.getTerrain("Foxholes");
                currentTerrainName	= "Foxholes";
            }
        }
        repaint();
    }

    public void setCurrentTerrain(String newCurrentTerrain) {

        if (losDataEditor == null) {
            return;
        }

        currentTerrainName = newCurrentTerrain;

        final Map map = losDataEditor.getMap();

        if ("Set ground level".equals(currentFunctionName)) {

            // set the current ground level
            if ("Hill Level 0".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 0;
            } else if ("Hill Level 1".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 1;
            } else if ("Hill Level 2".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 2;
            } else if ("Hill Level 3".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 3;
            } else if ("Hill Level 4".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 4;
            } else if ("Hill Level 5".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 5;
            } else if ("Hill Level 6".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 6;
            } else if ("Hill Level 7".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 7;
            } else if ("Hill Level 8".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 8;
            } else if ("Hill Level 9".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 9;
            } else if ("Hill Level 10".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = 10;
            } else if ("Valley -1".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = -1;
            } else if ("Valley -2".equals(currentTerrainName)) {
                currentTerrain = null;
                currentGroundLevel = -2;
            } else if ("Gully".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Gully");
                currentGroundLevel = -1;
            } else if ("Dry Stream".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Dry Stream");
                currentGroundLevel = -1;
            } else if ("Shallow Stream".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Shallow Stream");
                currentGroundLevel = -1;
            } else if ("Deep Stream".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Deep Stream");
                currentGroundLevel = -1;
            } else if ("Wadi".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Wadi");
                currentGroundLevel = -1;
            }
        }

        else if ("Add hexside terrain".equals(currentFunctionName)) {

            final Terrain t = map.getTerrain(currentTerrainName);

            if ("Remove".equals(currentTerrainName)) {
                currentTerrain = null;
            } else if (t == null) {

                frame.setStatusBarText("Terrain " + currentTerrainName + " not found. Terrain set to 'Wall'.");
                currentTerrain = map.getTerrain("Wall");
                currentTerrainName = "Wall";
            } else {

                currentTerrain = t;
            }
        } else if ("Add terrain".equals(currentFunctionName)) {

            final Terrain t = map.getTerrain(currentTerrainName);

            if (t == null) {

                frame.setStatusBarText("Terrain " + currentTerrainName + " not found. Terrain set to 'Open Ground'.");
                currentTerrain = map.getTerrain("Open Ground");
                currentTerrainName = "Open Ground";
            } else {

                currentTerrain = t;
            }

        } else if ("Add bridge".equals(currentFunctionName)) {


            if ("Remove".equals(currentTerrainName)) {
                currentTerrain = null;
                currentBridge = null;
            } else if ("Single Hex Wooden Bridge".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Single Hex Wooden Bridge");
                currentBridge = new Bridge(currentTerrain, customBridgeRoadElevation, rotation, null, true);
            } else if ("Single Hex Stone Bridge".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Single Hex Stone Bridge");
                currentBridge = new Bridge(currentTerrain, customBridgeRoadElevation, rotation, null, true);
            } else if ("Wooden Bridge".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Wooden Bridge");
                currentBridge = new Bridge(currentTerrain, customBridgeRoadElevation, rotation, null, true);
            } else if ("Stone Bridge".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Stone Bridge");
                currentBridge = new Bridge(currentTerrain, customBridgeRoadElevation, rotation, null, true);
            }
        } else if ("Add road".equals(currentFunctionName)) {

            if ("Dirt Road".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Dirt Road");
            } else if ("Paved Road".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Paved Road");
            } else if ("Elevated Road".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Elevated Road");
            } else if ("Sunken Road".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Sunken Road");
            } else if ("Runway".equals(currentTerrainName)) {
                currentTerrain = map.getTerrain("Runway");
            }
        }
        else if ("Add objects".equals(currentFunctionName)) {

            if("Foxholes".equals(currentTerrainName)){
                currentTerrain = map.getTerrain("Foxholes");
            }
            else if("Trench".equals(currentTerrainName)){
                currentTerrain = map.getTerrain("Trench");
            }
            else if("Tunnel".equals(currentTerrainName)){
                currentTerrain = map.getTerrain("Tunnel");
            }
            else if("Sewer".equals(currentTerrainName)){
                currentTerrain = map.getTerrain("Sewer");
            }
            else if("Stairway".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Smoke".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Vehicle".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Remove Stairway".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Remove Tunnel/Sewer".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Remove Entrenchment".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Remove Smoke".equals(currentTerrainName)){
                currentTerrain = null;
            }
            else if("Remove Vehicle".equals(currentTerrainName)){
                currentTerrain = null;
            }
        }
        frame.setStatusBarText("  ");
        repaint();
    }

    public String getCurrentTerrain() {

        return currentTerrainName;
    }

    public void setCurrentToTerrain(String newCurrentToTerrain) {

        currentToTerrainName = newCurrentToTerrain;

        final Map map = losDataEditor.getMap();

        if ("Set ground level".equals(currentFunctionName)) {

            // set the current ground level
            if ("Hill Level 0".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 0;
            } else if ("Hill Level 1".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 1;
            } else if ("Hill Level 2".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 2;
            } else if ("Hill Level 3".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 3;
            } else if ("Hill Level 4".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 4;
            } else if ("Hill Level 5".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 5;
            } else if ("Hill Level 6".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 6;
            } else if ("Hill Level 7".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 7;
            } else if ("Hill Level 8".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 8;
            } else if ("Hill Level 9".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 9;
            } else if ("Hill Level 10".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = 10;
            } else if ("Valley -1".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = -1;
            } else if ("Valley -2".equals(currentToTerrainName)) {
                currentToTerrain = null;
                currentToGroundLevel = -2;
            } else if ("Gully".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Gully");
                currentToGroundLevel = -1;
            } else if ("Dry Stream".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Dry Stream");
                currentToGroundLevel = -1;
            } else if ("Shallow Stream".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Shallow Stream");
                currentToGroundLevel = -1;
            } else if ("Deep Stream".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Deep Stream");
                currentToGroundLevel = -1;
            }
        } else if ("Add hexside terrain".equals(currentFunctionName)) {

            final Terrain t = map.getTerrain(currentToTerrainName);

            if (t == null) {

                frame.setStatusBarText("Terrain " + currentToTerrainName + " not found. Terrain set to 'Wall'.");
                currentToTerrain = map.getTerrain("Wall");
                currentToTerrainName = "Wall";
            } else {

                currentToTerrain = t;
            }
        } else if ("Add terrain".equals(currentFunctionName)) {

            final Terrain t = map.getTerrain(currentToTerrainName);

            if (t == null) {

                frame.setStatusBarText("Terrain " + currentToTerrainName + " not found. Terrain set to 'Open Ground'.");
                currentToTerrain = map.getTerrain("Open Ground");
                currentToTerrainName = "Open Ground";
            } else {

                currentToTerrain = t;
            }
        } else if ("Add road".equals(currentFunctionName)) {

            if ("Dirt Road".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Dirt Road");
            } else if ("Paved Road".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Paved Road");
            } else if ("Elevated Road".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Elevated Road");
            } else if ("Sunken Road".equals(currentToTerrainName)) {
                currentToTerrain = map.getTerrain("Sunken Road");
            }
        }

        frame.setStatusBarText("  ");
        repaint();
    }

    public void setCurrentBrush(String newCurrentBrush) {

        if (newCurrentBrush == null) {
            currentBrush = "";
        } else {
            currentBrush = newCurrentBrush;
        }

        if (currentBrush.isEmpty()) {
            currentBrushSize = 0;
        } else if ("1  Pixel".equals(currentBrush)) {
            currentBrushSize = 1;
        } else if ("2  Pixel".equals(currentBrush)) {
            currentBrushSize = 2;
        } else if ("4  Pixel".equals(currentBrush)) {
            currentBrushSize = 4;
        } else if ("8  Pixel".equals(currentBrush)) {
            currentBrushSize = 8;
        } else if ("16 Pixel".equals(currentBrush)) {
            currentBrushSize = 16;
        } else if ("32 Pixel".equals(currentBrush)) {
            currentBrushSize = 32;
        } else if ("64 Pixel".equals(currentBrush)) {
            currentBrushSize = 64;
        } else if ("Hex".equals(currentBrush)) {
            currentBrushSize = -1;
        }
        repaint();
    }

    public void mapToggleButton(){
        VASLImage = !VASLImage;
        repaint();
    }

    public void updateMap() {

        // is the map open?
        if (!mapOpen) return;

        if ("Set ground level".equals(currentFunctionName)) {

            // set the map grid first...
            Iterator iterator = allSelections.iterator();
            while (iterator.hasNext()) {

                final Shape s = ((Selection) iterator.next()).getUpdateShape();

                losDataEditor.setGridGroundLevel(s, currentTerrain, currentGroundLevel);
                mapChanged = true;
                setDirtyArea(s.getBounds());
            }

            // ...then set the hex elevation/depression info
            iterator = allSelections.iterator();
            while (iterator.hasNext()) {

                losDataEditor.setHexGroundLevel(((Selection) iterator.next()).getUpdateShape(), currentTerrain, currentGroundLevel);
            }
        }
        else if ("Add hexside terrain".equals(currentFunctionName)) {

            HexsideSelection selectedHexside;
            int hexside;
            Hex hex;

            for (Selection allSelection : allSelections) {

                selectedHexside = (HexsideSelection) allSelection;
                hex = selectedHexside.getLocation().getHex();

                // set the hexside terrain in the location hex
                hexside = hex.getLocationHexside(selectedHexside.getLocation());
                hex.setHexsideTerrain(hexside, currentTerrain);

                // set the hexside terrain in the adjacent hex
                hex = losDataEditor.getMap().getAdjacentHex(hex, hexside);
                if (hex != null) {
                    hex.setHexsideTerrain(hex.getOppositeHexside(hexside), currentTerrain);
                }

                // set the grid map - use open ground when removing terrain
                if (currentTerrain == null) {
                    losDataEditor.setGridTerrain(selectedHexside.getUpdateShape(), losDataEditor.getMap().getTerrain("Open Ground"));
                } else {
                    losDataEditor.setGridTerrain(selectedHexside.getUpdateShape(), currentTerrain);
                }
                setDirtyArea(selectedHexside.getUpdateShape().getBounds());
                mapChanged = true;
            }
        }
        else if ("Add terrain".equals(currentFunctionName)) {

            Selection sel;

            for (Selection allSelection : allSelections) {

                sel = allSelection;
                final Shape s = sel.getUpdateShape();

                losDataEditor.setGridTerrain(s, currentTerrain);
                losDataEditor.setHexTerrain(s, currentTerrain);
                mapChanged = true;
                setDirtyArea(s.getBounds());

            }
        }
        else if ("Add bridge".equals(currentFunctionName)) {
            if (!allSelections.isEmpty()) {

                // selected bridge
                final Iterator iterator = allSelections.iterator();
                while (iterator.hasNext()) {

                    // remove?
                    if (currentTerrain == null) {


                        final Hex h = ((HexSelection) iterator.next()).getHex();
                        // h.removeBridge();

                        // update the map
                        setDirtyArea(h.getHexBorder().getBounds());
                        mapChanged = true;
                    }

                    // add the bridge
                    else {

                        final BridgeSelection sel = (BridgeSelection) iterator.next();

                        // Bridge location is currently the center of the hex
                        // need to create the new
                        sel.getHex().setBridge(sel.getBridge());

                        // update the map
                        setDirtyArea(sel.getUpdateShape().getBounds());
                        mapChanged = true;
                    }
                }
            }
        }
        else if ("Add road".equals(currentFunctionName)) {
            Iterator iter;
            HexsideSelection selectedHexside;
            Terrain tempTerrain = currentTerrain;

            // set depression/groundlevel for sunken/elevated road
            if ("Elevated Road".equals(currentTerrain.getName()) || "Sunken Road".equals(currentTerrain.getName())) {

                // convert sunken road to dirt roads for non-depression terrain
                if ("Sunken Road".equals(currentTerrain.getName())) {
                    tempTerrain = losDataEditor.getMap().getTerrain("Dirt Road");
                }

                // set the map grid first...
                iter = allSelections.iterator();
                while (iter.hasNext()) {

                    selectedHexside = (HexsideSelection) iter.next();

                    if ("Elevated Road".equals(currentTerrain.getName())) {

                        losDataEditor.setGridGroundLevel(
                                selectedHexside.getUpdateShape(),
                                null,
                                1);
                    } else {
                        losDataEditor.setGridGroundLevel(
                                selectedHexside.getUpdateShape(),
                                currentTerrain,
                                0);
                    }
                }

                // ...then set the hex elevation/depression info
                iter = allSelections.iterator();
                while (iter.hasNext()) {

                    selectedHexside = (HexsideSelection) iter.next();

                    if ("Elevated Road".equals(currentTerrain.getName())) {

                        losDataEditor.setHexGroundLevel(
                                selectedHexside.getUpdateShape(),
                                null,
                                1);
                    } else {
                        losDataEditor.setHexGroundLevel(
                                selectedHexside.getUpdateShape(),
                                currentTerrain,
                                0);
                    }
                }
            }

            iter = allSelections.iterator();
            while (iter.hasNext()) {

                selectedHexside = (HexsideSelection) iter.next();

                losDataEditor.setGridTerrain(selectedHexside.getPaintShape(), tempTerrain);
                losDataEditor.setHexTerrain(selectedHexside.getPaintShape(), tempTerrain);

                setDirtyArea(selectedHexside.getUpdateShape().getBounds());
                mapChanged = true;
            }
        }

        else if ("Add objects".equals(currentFunctionName)) {

            if (!allSelections.isEmpty()) {

                final Iterator iter = allSelections.iterator();
                Hex h = null;
                while(iter.hasNext()){

                    h = ((HexSelection) iter.next()).getHex();

                    if("Stairway".equals(currentTerrainName)){

                        h.setStairway(true);
                    }
                    else if("Remove Stairway".equals(currentTerrainName)){

                        h.setStairway(false);
                    }

                    // adjust "dirty" area of map
                    setDirtyArea(h.getExtendedHexBorder().getBounds());
                    mapChanged = true;
                }
            }
        }
        // rebuild the map
        // losDataEditor.getMap().resetHexTerrain();
        paintMapImage();
        clearSelections();
        repaint();
    }

    public void setRoundBrush(boolean isRoundBrush) {

        roundBrush = isRoundBrush;
    }

    public void setVASLImage(boolean VASLImage) {
        this.VASLImage = VASLImage;
    }

    public void clearSelections() {

        if (!allSelections.isEmpty()) {
            allSelections.clear();
        }
        // reset the dirty area
        minDirtyX = -1;
        minDirtyY = -1;
        maxDirtyX = -1;
        maxDirtyY = -1;
    }

    public void createNewMap() {

        // create the map
        frame.setStatusBarText("This option is not currently supported. Open an existing board. ");
        frame.paintImmediately();
    }

    public void saveLOSData() {

        frame.setStatusBarText("Saving the map...");
        frame.paintImmediately();
        losDataEditor.saveLOSData();
        mapChanged = false;
        frame.setStatusBarText("");
        frame.paintImmediately();

    }

    public void openMap() {

        // create the map image
        frame.setStatusBarText("Creating the map image...");
        frame.paintImmediately();
        mapImage = new BufferedImage(losDataEditor.getMap().getGridWidth(), losDataEditor.getMap().getGridHeight(), BufferedImage.TYPE_3BYTE_BGR);
        paintMapImage();
        frame.setStatusBarText("  ");
        adjustMapViewSize();
        mapOpen = true;
        mapChanged = false;
        frame.setStatusBarText("  ");
        sourceLocation = losDataEditor.getMap().getHex(losDataEditor.getMap().getWidth() / 2, 1).getCenterLocation();
        targetLocation = sourceLocation;

    }

    /**
     * Open a VASL archive for editing LOS data
     * @param archiveName fully qualified name of the board archive
     */
    public void openArchive(String archiveName) {

        try {
            losDataEditor = new GUILOSDataEditor(
                    archiveName,
                    LOSEditorProperties.getBoardDirectory(),
                    sharedBoardMetadata);
        } catch (IOException e) {
            System.err.println("Cannot open the board archive: " + archiveName);
            e.printStackTrace();
        }

        // load the terrain images
        loadTerrainGraphics();

        // try to open LOS data
        frame.setStatusBarText("Reading or creating the LOS data...");
        losDataEditor.readLOSData();

        // create an empty geo board if no LOS data - for now
        if(losDataEditor.getMap() == null) {
            createNewMap();
        }
        openMap();
    }

    public void closeMap() {

        // reset the map
        mapChanged = false;
        mapOpen = false;
        losDataEditor = null;
        mapImage = null;
        frame.setStatusBarText("  ");
        adjustMapViewSize();
        repaint();

    }

    public void undoSelections() {

        clearSelections();
        repaint();
        mapChanged = false;
    }

    public void setCustomBridgeOn(boolean newCustomBuildingOn) {

        customBridgeOn = newCustomBuildingOn;
    }

    public void setBridgeParameters(String terr, int roadElevation) {

        // set the current terrain
        setCurrentTerrain(terr);

        // set the custom bridge parameters
        customBridgeRoadElevation = roadElevation;
    }

    public int getBridgeRoadElevation() {
        return customBridgeRoadElevation;
    }

    public void setRotation(int rotation) {

        this.rotation = rotation;

    }

    /**
     * ***************************
     * Keyboard methods
     * ****************************
     */
    public void keyTyped(KeyEvent e) {

    }

    public void keyReleased(KeyEvent e) {

    }

    public void keyPressed(KeyEvent e) {

        final int code = e.getKeyCode();
        final String modifiers = KeyEvent.getKeyModifiersText(e.getModifiers());

        // is the map open?
        if (!mapOpen) return;


            // toggle map
        if (code == KeyEvent.VK_V) {

            mapToggleButton();
        }

        // Not doing LOS?
        if (!"LOS".equals(currentFunctionName)) {

            // undo
            if (code == KeyEvent.VK_Z && "Ctrl".equals(modifiers)) {

                if (!allSelections.isEmpty()) {

                    allSelections.remove(allSelections.getLast());
                    repaint();
                }
            }
            // clear selections
            else if (code == KeyEvent.VK_ESCAPE) {

                clearSelections();
            }

            // update
            else if (code == KeyEvent.VK_U) {

                updateMap();
            }
        }
    }

    public void paintMapImage() {

        final Map map = losDataEditor.getMap();
        losDataEditor.paintMapArea(0, 0, map.getGridWidth(), map.getGridHeight(), mapImage, terrainImages, singleHexWoodenBridgeImage, singleHexStoneBridgeImage);
        losDataEditor.paintMapShadows(0, 0, map.getGridWidth(), map.getGridHeight(), mapImage);
        losDataEditor.paintMapContours(0, 0, map.getGridWidth(), map.getGridHeight(), mapImage);
        losDataEditor.paintMapHexes(mapImage);
        losDataEditor.paintAncillaryHexTerrain(mapImage);
    }

    // adjust "dirty" area of map
    private void setDirtyArea(Rectangle rect) {

        //first time?
        if (minDirtyX == -1) {

            minDirtyX = (int) rect.getX();
            minDirtyY = (int) rect.getY();
            maxDirtyX = (int) (rect.getX() + rect.getWidth());
            maxDirtyY = (int) (rect.getY() + rect.getHeight());
        } else {

            minDirtyX = (int) Math.min(rect.getX(), (double)minDirtyX);
            minDirtyY = (int) Math.min(rect.getY(), (double)minDirtyY);
            maxDirtyX = (int) Math.max(rect.getX() + rect.getWidth(), (double)maxDirtyX);
            maxDirtyY = (int) Math.max(rect.getY() + rect.getHeight(), (double)maxDirtyY);
        }
    }

    public void   changeAllTerrain() {

        final boolean changed;

        frame.setStatusBarText("Changing the map...");
        frame.paintImmediately();

        // just the current selections?
        if (!allSelections.isEmpty()) {

            // update the map
            if (currentTerrain != null && currentToTerrain != null) {

                for (Selection s : allSelections) {

                    final boolean selectionChanged = losDataEditor.changeAllTerrain(currentTerrain, currentToTerrain, s.getUpdateShape());

                    if (selectionChanged) {

						setDirtyArea(s.getUpdateShape().getBounds());
                        mapChanged = true;
                    }

                }
            } else if (currentTerrain == null && currentToTerrain == null) {

                for (Selection s : allSelections) {

                    final boolean selectionChanged = losDataEditor.changeAllGroundLevel(currentGroundLevel, currentToGroundLevel, s.getUpdateShape());

                    if (selectionChanged) {

						setDirtyArea(s.getUpdateShape().getBounds());
                        mapChanged = true;
                    }

                }
            } else {
                frame.setStatusBarText("Illegal terrain mapping");
                return;
            }

            // clear the selections, set changed flag
            allSelections.clear();
        }

        // the whole map
        else {

            // update the map
            if (currentTerrain != null && currentToTerrain != null) {
                changed = losDataEditor.changeAllTerrain(currentTerrain, currentToTerrain);
            } else if (currentTerrain == null && currentToTerrain == null) {
                changed = losDataEditor.changeAllGroundLevel(currentGroundLevel, currentToGroundLevel);
            } else {
                frame.setStatusBarText("Illegal terrain mapping");
                return;
            }

            if (!changed) {

                frame.setStatusBarText("Nothing changed");
                return;

            }

            // mark the whole map as changed and recreate
            minDirtyX = -1;

        }

        frame.setStatusBarText("Recreating the map image...");
        losDataEditor.getMap().resetHexTerrain(0);
        frame.paintImmediately();
        paintMapImage();
        frame.setStatusBarText("");
        repaint();
    }

    public Image getImage(String imageName) {

        try {

            final ZipEntry e = archive.getEntry(imageName);
            final InputStream ip = archive.getInputStream(e);

            if (ip == null) {

                return null;
            } else {

                final byte[] bytes = new byte[ip.available()];
                final int count = ip.read(bytes);
                if(count > 0) {
                    final Image temp = Toolkit.getDefaultToolkit().createImage(bytes);

                    final MediaTracker m = new MediaTracker(this);
                    m.addImage(temp, 0);
                    m.waitForID(0);

                    return temp;
                }

                return null;
            }

        } catch (IOException e) {

            return null;

        } catch (InterruptedException e) {

            return null;

        } catch (Exception e) {

            return null;
        }
    }

    public InputStream getTextFile(String imageName) {

        try {

            final ZipEntry e = archive.getEntry(imageName);
            return archive.getInputStream(e);


        } catch (Exception e) {

            return null;
        }
    }

    public void flipMap() {

        frame.setStatusBarText("Flipping the map...");
        frame.paintImmediately();
        losDataEditor.flip();
        frame.setStatusBarText("Rebuilding the map image...");
        frame.paintImmediately();
        paintMapImage();
        mapChanged = true;
        frame.setStatusBarText("");
    }

    /**
     * Used for debugging individual LOS
     */
    public void runSingleLOS() {

        final Map map = losDataEditor.getMap();

        final LOSResult result = new LOSResult();
        result.reset();
        map.LOS(map.getHex("O2").getCenterLocation(), false, map.getHex("D1").getCenterLocation(), false, result, null);

    }

    public void runLosTest() {

        final Map map = losDataEditor.getMap();

        final int width = map.getWidth();
        final int height = map.getHeight();
        int count = 0;
        int blocked = 0;

        final LOSResult result = new LOSResult();

        frame.setStatusBarText("Running the LOS test...");
        frame.paintImmediately();

        // set the start time and save the base height
        final long startTime = System.currentTimeMillis();

        // check all LOS on the board
        for (int col = 0; col < width; col++) {
            for (int row = 0; row < height + (col % 2); row++) {
                for(int loc = 0; loc <= 6; loc++) {

                    final Location l2;
                    if(loc == 6) {
                        l2 = map.getHex(col, row).getCenterLocation();
                    }
                    else {
                        l2 = map.getHex(col, row).getHexsideLocation(loc);
                    }

                    // all upper level locations
                    for(Location l3 = l2; l3 != null; l3 = l3.getUpLocation()) {
                        for (int col2 = 0; col2 < width && map.onMap(l3.getLOSPoint().x, l3.getLOSPoint().y); col2++) {
                            for (int row2 = 0; row2 < height + (col2 % 2); row2++) {

                                result.reset();
                                map.LOS(l3, false, map.getHex(col2, row2).getCenterLocation(), false, result, null);

                                // increment counters
                                count++;
                                if (result.isBlocked()) {

                                    blocked++;
                                }
                            }
                        }
                    }
                }
            }
        }

        frame.setStatusBarText(
                "LOS test complete. Total checks: " + count +
                        "  Blocked: " + (int) ((float) blocked / (float) count * 100.0F) + "%" +
                        "  Time elapsed: " + (((double) System.currentTimeMillis() - (double) startTime) / 1000.0));
        frame.paintImmediately();
    }

    public void insertMap(Map insertMap, String upperLeftHex) {

        if (losDataEditor.insertMap(insertMap, losDataEditor.getMap().getHex(upperLeftHex.toUpperCase()))) {

            frame.setStatusBarText("Rebuilding the map image...");
            frame.paintImmediately();

            paintMapImage();
            mapChanged = true;
            frame.setStatusBarText("");
        }
    }

}

