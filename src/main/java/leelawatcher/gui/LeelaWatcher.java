/*
    Copyright 2017 Patrick G. Heck

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package leelawatcher.gui;

import com.google.common.io.Resources;
import leelawatcher.goboard.Board;
import leelawatcher.parser.AutoGtpOutputParser;
import org.docopt.Docopt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public class LeelaWatcher {
  private Board currBoard = new Board();
  private AutoGtpOutputParser parser;

  private JTextArea leelaOutputTextArea;
  private JPanel top;
  private BoardView boardView;
  private JScrollPane textScrollPane;
  private JSplitPane splitPane;
  private static Process proc;

  // flags
  private static boolean dontSaveGames;
  private static boolean hideOutputWindow;

  private void createUIComponents() {
    boardView = new BoardView(currBoard);
  }

  public static void main(String[] args) throws IOException {
    URL usage = Resources.getResource("usage.docopts.txt");
    String doc = Resources.toString(usage, Charset.forName("UTF-8"));
    Docopt options = new Docopt(doc);
    Map<String, Object> optMap = options.parse(args);

    if (optMap.get("--no-sgf") != null) {
      dontSaveGames = true;
    }
    if (optMap.get("--board-only") != null) {
      hideOutputWindow = true;
    }


    LeelaWatcher leelaWatcher = new LeelaWatcher();
    JFrame frame = new JFrame();
    frame.setContentPane(leelaWatcher.$$$getRootComponent$$$());
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setTitle("Leela Watcher");
    frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          proc.destroyForcibly();
          super.windowClosing(e);
        }
      });

    if (hideOutputWindow) {
      leelaWatcher.leelaOutputTextArea.setRows(0);
      leelaWatcher.leelaOutputTextArea.setPreferredSize(new Dimension(0,0));
      leelaWatcher.splitPane.getRightComponent().setVisible(false);
      leelaWatcher.splitPane.setDividerSize(0);
    }

    frame.pack();
    frame.setVisible(true);
    SwingUtilities.invokeLater(() -> {
        try {
          //noinspection SpellCheckingInspection
          String cmd;
          Object cmdObj = optMap.get("<cmd>");
          if (cmdObj != null) {
            cmd = String.valueOf(cmdObj);
          } else {
            cmd = "./autogtp";
          }
          System.out.println("cmd is " + cmd);
          ProcessBuilder pb = new ProcessBuilder(cmd);
          pb.directory(new File(String.valueOf( optMap.get("<dir>"))));
          pb.redirectErrorStream(true);
          proc = pb.start();
          leelaWatcher.parser = new AutoGtpOutputParser(leelaWatcher.boardView);
          leelaWatcher.parser.addPropertyChangeListener(evt -> {
              if ("message".equals(evt.getPropertyName())) {
                JTextArea ta = leelaWatcher.leelaOutputTextArea;
                ta.setText(ta.getText() + evt.getNewValue());
                JScrollBar vertical = leelaWatcher.textScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
              }
              if ("inProgress".equals(evt.getPropertyName())) {
                if (Objects.equals(evt.getNewValue(), false)) {
                  if (!dontSaveGames) {
                    leelaWatcher.boardView.saveGame();
                  }
                }
              }
            });
          leelaWatcher.parser.start(new BufferedInputStream(proc.getInputStream()));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
  }

  {
    // GUI initializer generated by IntelliJ IDEA GUI Designer
    // >>> IMPORTANT!! <<<
    // DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    top = new JPanel();
    top.setLayout(new BorderLayout(0, 0));
    splitPane = new JSplitPane();
    splitPane.setOrientation(0);
    top.add(splitPane, BorderLayout.CENTER);
    splitPane.setLeftComponent(boardView);
    textScrollPane = new JScrollPane();
    splitPane.setRightComponent(textScrollPane);
    leelaOutputTextArea = new JTextArea();
    leelaOutputTextArea.setRows(4);
    textScrollPane.setViewportView(leelaOutputTextArea);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return top;
  }
}
