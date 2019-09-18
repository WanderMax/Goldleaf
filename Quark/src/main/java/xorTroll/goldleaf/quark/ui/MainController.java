package xorTroll.goldleaf.quark.ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import xorTroll.goldleaf.quark.Main;

public class MainController
{
    public class UILogger extends OutputStream
    {
        private JFXTextArea area;

        public UILogger(JFXTextArea area)
        {
            this.area = area;
        }

        public void writeToArea(String data)
        {
            Platform.runLater(() -> area.appendText(data));
        }

        public void write(int b) throws IOException
        {
            writeToArea(String.valueOf((char)b));
        }
    }

    @FXML
    private Label USBStatusLabel;

    @FXML
    private JFXProgressBar ProgressBar;

    @FXML
    private Pane MainPane;

    @FXML
    private JFXButton NewPathButton;

    @FXML
    private JFXButton PathRemoveButton;

    @FXML
    private JFXTextArea LogArea;

    @FXML
    private JFXListView<String> PathList;

    @FXML
    private JFXToggleButton ShowLogsToggle;

    private PrintStream Logger;
    private Main main;

    public void prepare(Main main, Task<Void> usbtask)
    {
        this.main = main;
        refreshConfig();
        
        NewPathButton.setOnAction(event ->
        {
            DirectoryChooser ch = new DirectoryChooser();
            File dir = ch.showDialog(this.main.stage);
            if(dir != null)
            {
                JFXDialogLayout lyt = new JFXDialogLayout();
                lyt.setHeading(new Text("Quark - Add special path"));
                lyt.setBody(new Text("Set the special path's name.\n(leave the box empty to cancel)"));

                JFXAlert<Void> al = new JFXAlert<>((Stage)MainPane.getScene().getWindow());
                al.initModality(Modality.APPLICATION_MODAL);
                al.setOverlayClose(false);

                JFXTextField tf = new JFXTextField();
                tf.setAlignment(Pos.BOTTOM_LEFT);

                JFXButton ok = new JFXButton("Ok");
                ok.setAlignment(Pos.BOTTOM_RIGHT);
                ok.setOnAction(event2 ->
                {
                    al.close();
                });

                lyt.setActions(tf, ok);
                al.setContent(lyt);
                al.showAndWait();

                String key = tf.getText();
                if((key != null) && !(key.trim().isEmpty()))
                {
                    synchronized(main.cfglock)
                    {
                        main.cfg.data.setProperty(key, dir.toString());
                        refreshConfig();
                        main.cfg.save();
                    }
                }
            }
        });
        PathRemoveButton.setOnAction(event ->
        {
            ObservableList<String> itms = PathList.getSelectionModel().getSelectedItems();
            if(!itms.isEmpty())
            {
                synchronized(main.cfglock)
                {
                    itms.forEach(itm ->
                    {
                        Enumeration<?> enums = main.cfg.data.propertyNames();
                        while(enums.hasMoreElements())
                        {
                            String key = (String)enums.nextElement();
                            String value = main.cfg.data.getProperty(key);
                            if(itm.equalsIgnoreCase(key + " (" + value + ")")) main.cfg.data.remove(key);
                        }
                    });
                    
                    refreshConfig();
                    main.cfg.save();
                }
            }
        });
        ShowLogsToggle.setSelected(false);
        LogArea.visibleProperty().bind(ShowLogsToggle.selectedProperty());
        Logger = new PrintStream(new UILogger(LogArea));
        System.setOut(Logger);
        USBStatusLabel.textProperty().bind(usbtask.messageProperty());
        ProgressBar.progressProperty().bind(usbtask.progressProperty());
    }

    public void refreshConfig()
    {
        PathList.getItems().clear();
        synchronized(main.cfglock)
        {
            main.cfg.data.forEach((key, path) ->
            {
                PathList.getItems().add(key + " (" + path + ")");
            });
        }
    }
}