/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vennmesh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Timer;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
    JFrame frame = new JFrame("VennMesh");
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
      dc.setSize(700, 700);
      panel.add(dc, BorderLayout.CENTER);

      //panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    frame.setSize(700, 700);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
  /* *************************************************************************************************** */
  public static class Drawing_Canvas extends javax.swing.JPanel {/* JFrame */

    BufferedImage Buffer;
    Graphics2D GlobalGraphics;
    Display display;
    Node TargetNode;
    /* *************************************************************************************************** */
    public Drawing_Canvas() {
      this.setName("Drawing_Canvas");
      display = new Display();
      if (false) {
        Rectangle screen = this.getBounds();
        Buffer = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_ARGB);
        GlobalGraphics = Buffer.createGraphics();
      }
      this.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
          if (SwingUtilities.isLeftMouseButton(evt)) {
            TargetNode = display.TriggerBlastPacket(evt.getX(), evt.getY());
          } else if (SwingUtilities.isRightMouseButton(evt)) {
            display.Seek(evt.getX(), evt.getY(), TargetNode.MeZone);
          } else if (SwingUtilities.isMiddleMouseButton(evt)) {
          } else {
          }
        }
        @Override
        public void mousePressed(java.awt.event.MouseEvent evt) {
        }
        @Override
        public void mouseReleased(java.awt.event.MouseEvent evt) {
        }
      });
      this.addMouseMotionListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseDragged(java.awt.event.MouseEvent evt) {
        }
        @Override
        public void mouseMoved(java.awt.event.MouseEvent evt) {
        }
      });
      this.setFocusable(true);
      this.addKeyListener(new KeyListener() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.isControlDown()) {
            char ch = (char) e.getKeyCode();// always uppercase
            System.out.println("ch[" + ch + "]");
            if (e.getKeyCode() == KeyEvent.VK_C) {
              System.out.println("Copy!");
            } else if (e.getKeyCode() == KeyEvent.VK_X) {
              System.out.println("Cut!");
            }
          } else {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {// just escape
              System.out.println("Quit!");
              System.exit(0);
            }
          }
        }
        @Override
        public void keyReleased(KeyEvent e) {
        }
        @Override
        public void keyTyped(KeyEvent e) {
        }
      });

      this.addComponentListener(new ComponentListener() {
        @Override
        public void componentResized(ComponentEvent evt) {/* This method is called after the component's size changes */
          Component c = (Component) evt.getSource();
          Dimension newSize = c.getSize();/* Get new size */
          Buffer = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_INT_ARGB);
          //Buffer = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_INT_RGB);
          GlobalGraphics = Buffer.createGraphics();
          //display.RandomizeAllConnections();
          /*
           display.BroadcastAllPackets();
           display.ProcessInPacketBuffer();
           display.Draw_Me(GlobalGraphics);
           */
        }
        @Override
        public void componentMoved(ComponentEvent e) {
        }
        @Override
        public void componentShown(ComponentEvent e) {
          boolean nop = true;
        }
        @Override
        public void componentHidden(ComponentEvent e) {
        }
      });

      if (false) {
        final Runnable doHelloWorld = new Runnable() {
          public void run() {
            System.out.println("Hello World on " + Thread.currentThread());
          }
        };
        Thread appThread = new Thread() {
          public void run() {
            try {
              SwingUtilities.invokeAndWait(doHelloWorld);
            } catch (Exception e) {
              e.printStackTrace();
            }
            System.out.println("Finished on " + Thread.currentThread());
          }
        };
        appThread.start();
      }
    }
    /* *************************************************************************************************** */
    @Override
    public void paintComponent(Graphics g) {
      if (this.Buffer == null) {
        return;
      }
      //display.RandomizeAllConnections();
      {
        display.BroadcastAllPackets();
        display.ProcessInPacketBuffer();
      }
      Rectangle screen = this.getBounds();// transformationContext.getScreen();
      //Color bg = new Color(0.5f, 0.5f, 0.5f);
      //Color bg = new Color(200, 255, 200);// nice light green
      Color bg = new Color(230, 230, 230);// light gray
      GlobalGraphics.setColor(bg);//Color.LIGHT_GRAY);
      GlobalGraphics.fillRect(0, 0, (int) screen.getWidth(), (int) screen.getHeight());
      display.Draw_Me(GlobalGraphics);
      Graphics2D g2 = (Graphics2D) g;
      g2.drawImage(this.Buffer, null, this);
      //http://www.realapplets.com/tutorial/DoubleBuffering.html
      /* http://download.oracle.com/javase/tutorial/2d/images/drawonimage.html */
      try {
        //Thread.sleep(500);
      } catch (Exception ex) {
      }
      repaint(); // Repaint indirectly calls paintComponent.
      VennMesh.IncrementTimeStamp();
    }
    /* *************************************************************************************************** */
    @Override
    public void update(Graphics g) {
      paint(g);
    }
  }
  public static int CurrentId = 0;
  public static int GetNewId() {
    return CurrentId++;
  }
  public static int CurrentTime = 0;
  public static int GetTimeStamp() {
    return CurrentTime;
  }
  public static void IncrementTimeStamp() {
    CurrentTime++;
  }
}
