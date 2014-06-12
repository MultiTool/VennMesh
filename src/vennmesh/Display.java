package vennmesh;

/**

 @author MultiTool
 */
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Random;

/* ********************************************************************************************************* */
public class Display {// 2d grid of nodes
  int GridWdt = 10, GridHgt = 10;
  //double ConnectionDensity = 0.2;
  double ConnectionDensity = 1.0;
  List<Node> Nodes;
  Random rand = new Random();
  /* ********************************************************************************************************* */
  public Display() {
    double NodeSpacing = 3.0;
    Node node;
    Nodes = new ArrayList<Node>();
    int NodeNum = this.GridWdt * this.GridHgt;
    for (int vcnt = 0; vcnt < GridHgt; vcnt++) {
      for (int hcnt = 0; hcnt < GridWdt; hcnt++) {
        node = new Node();
        Nodes.add(node);
        node.Xloc = hcnt * NodeSpacing;
        node.Yloc = vcnt * NodeSpacing;
      }
    }
    ConnectAll();
  }
  /* ********************************************************************************************************* */
  public void ConnectAll() {
    int X0, Y0, X1, Y1;
    int XMax = this.GridWdt - 1;
    int YMax = this.GridHgt - 1;
    int radius = 1;
    for (int vcnt = 0; vcnt < this.GridHgt; vcnt++) {
      Y0 = Math.max(vcnt - radius, 0);// clip Y
      Y1 = Math.min(vcnt + radius, YMax);
      for (int hcnt = 0; hcnt < this.GridWdt; hcnt++) {
        X0 = Math.max(hcnt - radius, 0);// clip X
        X1 = Math.min(hcnt + radius, XMax);
        ConnectRegion(X0, Y0, hcnt, vcnt, X1, Y1);
      }
    }
  }
  /* ********************************************************************************************************* */
  public void ConnectRegion(int X0, int Y0, int XLoc, int YLoc, int X1, int Y1) {// Sparsely connect a node with its neighbors.
    Node ctr = GetNodeFromXY(XLoc, YLoc);// get this from xloc and yloc
    for (int vcnt = Y0; vcnt < Y1; vcnt++) {
      for (int hcnt = X0; hcnt < X1; hcnt++) {
        if (!((XLoc == hcnt) && (YLoc == vcnt))) {// do not connect to self
          if (rand.nextDouble() <= ConnectionDensity) {
            Node nbr = GetNodeFromXY(hcnt, vcnt);
            Connect2Nodes(ctr, nbr);
          }
        }
      }
    }
  }
  /* ********************************************************************************************************* */
  public void Connect2Nodes(Node node0, Node node1) {
    node0.AttachNeighbor(node1);
    node1.AttachNeighbor(node0);
  }
  /* ********************************************************************************************************* */
  public void Disconnect2Nodes(Node node0, Node node1) {
    node0.DisconnectNeighbor(node1);
    node1.DisconnectNeighbor(node0);
  }
  /* ********************************************************************************************************* */
  public Node GetNodeFromXY(int XLoc, int YLoc) {
    int Dex = YLoc * this.GridWdt + XLoc;
    return Nodes.get(Dex);
  }
  /* ********************************************************************************************************* */
  public void Draw_Me(Graphics2D g2d) {// Drawable
    AntiAlias(g2d);
    g2d.setColor(Color.black);
    for (int vcnt = 0; vcnt < this.GridHgt; vcnt++) {
      for (int hcnt = 0; hcnt < this.GridWdt; hcnt++) {
        Node nbr = GetNodeFromXY(hcnt, vcnt);
        nbr.Draw_Me(g2d);
      }
    }
  }
  /* ********************************************************************************************************* */
  public void AntiAlias(Graphics2D g2d) {//http://www.exampledepot.com/egs/java.awt/AntiAlias.html?l=rel
    RenderingHints rhints = g2d.getRenderingHints();// Determine if antialiasing is enabled
    Boolean antialiasOn = rhints.containsValue(RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);// Enable antialiasing for shapes
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);// Enable antialiasing for text
  }
  /* ********************************************************************************************************* */
}
