package VASL.LOSGUI;

import VASSAL.configure.DirectoryConfigurer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class PropertiesDialog extends JDialog {
    private JPanel panel1 = new JPanel();
    private JPanel panel2 = new JPanel();
    private JButton button1 = new JButton();
    private JButton button2 = new JButton();
    private JButton selectBoardDirectoryButton = new JButton();
    private JButton selectSBMDFileButton = new JButton();
    private Border border1;
    private JPanel jPanel1 = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private GridLayout gridLayout1 = new GridLayout();
    private JLabel jLabel1 = new JLabel();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private JLabel jLabel5 = new JLabel();
    //private JComboBox terrainComboBox = new JComboBox();
    private LOSEditorJFrame frame;

    // properties variables
    private String boardDirectory;
    private String sBMDfile;

    private JLabel jLabel6 = new JLabel();
    private JTextField boardDirTextField = new JTextField();
    private JTextField sBMDfileTextField = new JTextField();

    public PropertiesDialog(Frame frame, String title, boolean modal) {
        super(frame, title, modal);

        this.frame = (LOSEditorJFrame) frame;

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        pack();
    }

    private void jbInit() throws Exception {
        border1 = BorderFactory.createRaisedBevelBorder();
        jPanel1.setLayout(gridLayout1);
        panel2.setBorder(border1);
        panel2.setMaximumSize(new Dimension(500, 300));
        panel2.setMinimumSize(new Dimension(500, 300));
        panel2.setPreferredSize(new Dimension(500, 300));
        panel2.setLayout(gridBagLayout2);
        button1.setText("OK");
        button1.addActionListener(new PropertiesDialog_button1_actionAdapter(this));
        button2.setText("Cancel");
        gridLayout1.setHgap(4);
        button2.addActionListener(new PropertiesDialog_button2_actionAdapter(this));
        this.addWindowListener(new PropertiesDialog_this_windowAdapter(this));
        panel1.setLayout(gridBagLayout1);
        jLabel1.setText("Select Properties");
        jLabel1.setVerticalAlignment(SwingConstants.TOP);
        jLabel1.setVerticalTextPosition(SwingConstants.TOP);
        selectBoardDirectoryButton.setText("Select Board Directory:");
        selectBoardDirectoryButton.addActionListener(new PropertiesDialog_selectBoardDirectoryButton_actionAdapter(this));
        panel1.setMinimumSize(new Dimension(400, 220));
        panel1.setMaximumSize(new Dimension(400, 220));
        boardDirTextField.setText(LOSEditorProperties.getBoardDirectory());
        boardDirTextField.setMaximumSize(new Dimension(300, 24));
        boardDirTextField.setMinimumSize(new Dimension(200, 24));
        selectSBMDFileButton.setText("Select SBMD File:");
        selectSBMDFileButton.addActionListener(new PropertiesDialog_selectSBMDFileButton_actionAdapter(this));
        sBMDfileTextField.setText(LOSEditorProperties.getShardBoardMetadataFileName());
        sBMDfileTextField.setMaximumSize(new Dimension(300, 24));
        sBMDfileTextField.setMinimumSize(new Dimension(200, 24));
        panel1.add(panel2, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        panel2.add(jLabel1, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1, 3, 17, 147), 0, 0));
        panel2.add(sBMDfileTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 41, 0));
        panel2.add(boardDirTextField, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(2, 0, 1, 0), 51, 0));
        panel2.add(selectBoardDirectoryButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(9, 0, 1, 0), 6, 0));
        panel2.add(selectSBMDFileButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 11, 0));
        panel1.add(jPanel1, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 8, 4, 8), 0, 0));
        jPanel1.add(button1, null);
        jPanel1.add(button2, null);
        getContentPane().add(panel1);

    }

    // OK
    void button1_actionPerformed(ActionEvent e) {
       LOSEditorProperties loseditorprops = new LOSEditorProperties();
       loseditorprops.setBoardDirectory(boardDirTextField.getText());
       dispose();
    }

    // Cancel
    void button2_actionPerformed(ActionEvent e) {
        LOSEditorProperties loseditorprops = new LOSEditorProperties();
        loseditorprops.setShardBoardMetadataFileName(sBMDfileTextField.getText());
        dispose();
    }

    void selectBoardDirectoryButton_actionPerformed(ActionEvent e) {
        showChooserDialog("Boards");
    }

    void selectSBMDFileButton_actionPerformed(ActionEvent e) {
        showChooserDialog("SBMD");
    }
    private void showChooserDialog(String option) {
        // Create a JFrame as the parent for the dialog (can be null for a simple example)
        JFrame parentFrame = new JFrame();

        JFileChooser fileChooser = new JFileChooser();

        // Set the file selection mode to directories only
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Set the initial directory (e.g., the user's home directory)
        // You can replace "user.home" with any desired path string.
        String initialDirectoryPath ="";
        if (option.equals("SBMD")) {
            initialDirectoryPath = LOSEditorProperties.getShardBoardMetadataFileName();
        }
        else if (option.equals("Boards")) {
            initialDirectoryPath = LOSEditorProperties.getBoardDirectory();
        }
        fileChooser.setCurrentDirectory(new File(initialDirectoryPath));

        // Show the dialog
        int result = fileChooser.showOpenDialog(parentFrame);

        // Process the user's selection
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            LOSEditorProperties loseditorprops = new LOSEditorProperties();
            loseditorprops.setBoardDirectory(selectedFolder.getAbsolutePath());
            boardDirTextField.setText(selectedFolder.getAbsolutePath());
            //System.out.println("Selected directory: " + selectedFolder.getAbsolutePath());
        } else {
            System.out.println("No directory selected.");
        }
        //Center the dialog box
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = fileChooser.getSize();
        if (frameSize.height > screenSize.height)	frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width)  	frameSize.width = screenSize.width;
        fileChooser.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        fileChooser.setVisible(true);

    }

    void this_windowClosing(WindowEvent e) {
        dispose();
    }

    void heightTextField_actionPerformed(ActionEvent e) {

    }

    public void add(DirectoryConfigurer config) {
    }
}

class PropertiesDialog_button1_actionAdapter implements java.awt.event.ActionListener {
    PropertiesDialog adaptee;

    PropertiesDialog_button1_actionAdapter(PropertiesDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.button1_actionPerformed(e);
    }
}

class PropertiesDialog_button2_actionAdapter implements ActionListener {
    PropertiesDialog adaptee;

    PropertiesDialog_button2_actionAdapter(PropertiesDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.button2_actionPerformed(e);
    }
}

class PropertiesDialog_selectBoardDirectoryButton_actionAdapter implements java.awt.event.ActionListener {
    PropertiesDialog adaptee;

    PropertiesDialog_selectBoardDirectoryButton_actionAdapter(PropertiesDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.selectBoardDirectoryButton_actionPerformed(e);
    }
}

class PropertiesDialog_selectSBMDFileButton_actionAdapter implements java.awt.event.ActionListener {
    PropertiesDialog adaptee;

    PropertiesDialog_selectSBMDFileButton_actionAdapter(PropertiesDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.selectSBMDFileButton_actionPerformed(e);
    }
}

class PropertiesDialog_this_windowAdapter extends WindowAdapter {
    PropertiesDialog adaptee;

    PropertiesDialog_this_windowAdapter(PropertiesDialog adaptee) {
        this.adaptee = adaptee;
    }

    public void windowClosing(WindowEvent e) {
        adaptee.this_windowClosing(e);
    }


}
