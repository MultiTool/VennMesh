/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vennmesh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**

 @author MultiTool
 */
public class VennMesh {
  /**
   @param args the command line arguments
   */
  public static void main(String[] args) {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        createAndShowGUI();
      }
    });
  }
  /* *************************************************************************************************** */
  private static void createAndShowGUI() {
    // http://docs.oracle.com/javase/tutorial/uiswing/examples/start/HelloWorldSwingProject/src/start/HelloWorldSwing.java
    //Create and set up the window.
    JFrame frame = new JFrame("Fretless");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // capture global keyboard events, such as quit (ctrl-q) or save (ctrl-s). 
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
      @Override  // http://stackoverflow.com/questions/5344823/how-can-i-listen-for-key-presses-within-java-swing-accross-all-components
      public boolean dispatchKeyEvent(KeyEvent e) {
        // System.out.println("Got key event!");
        System.out.println("getKeyChar[" + e.getKeyChar() + "] getKeyCode[" + e.getKeyCode() + "] getModifiers[" + e.getModifiers() + "]");
        if (e.isControlDown()) {// cntrl-Q
          char ch = (char) e.getKeyCode();// always uppercase
          System.out.println("ch:" + ch + ":");
          if (e.getKeyCode() == KeyEvent.VK_Q) {
            System.out.println("Quit!");
            System.exit(0);
          }
        } else {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {// just escape
            System.out.println("Quit!");
            System.exit(0);
          }
        }
        return false;// false allows other key events to be triggered, too. 
      }
    });

    JPanel MainPanel = new JPanel();
    frame.getContentPane().add(MainPanel);

    //Display the window.
    frame.pack();
    frame.setVisible(true);

    MainPanel.setBackground(Color.CYAN);

    if (true) {// http://zetcode.com/tutorials/javaswingtutorial/basicswingcomponents/
      JPanel panel = MainPanel;// new JPanel();
      panel.setLayout(new BorderLayout(10, 10));

      Drawing_Canvas dc = new Drawing_Canvas();

      dc.setBackground(Color.red);
      dc.setSize(700, 600);
      panel.add(dc, BorderLayout.CENTER);

      //panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    frame.setSize(700, 600);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
  /* *************************************************************************************************** */
  public static class Drawing_Canvas extends javax.swing.JPanel {/* JFrame */

  }
}
