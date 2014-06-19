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
  int GridWdt = 16, GridHgt = 16;
  //double ConnectionDensity = 0.2;
  double ConnectionDensity = 0.5;
  //double ConnectionDensity = 1.0;
  double RandomChangeRate;
  int MaxConnectionRadius = 1;
  int XOrg, YOrg;
  List<Node> Nodes;
  public static Random rand = new Random();
  /* ********************************************************************************************************* */
  public Display() {
    XOrg = YOrg = 16;
    GridWdt = GridHgt = 16;
    double NodeSpacing = 30.0;
    Node node;
    Nodes = new ArrayList<Node>();
    int NodeNum = this.GridWdt * this.GridHgt;
    for (int vcnt = 0; vcnt < GridHgt; vcnt++) {
      for (int hcnt = 0; hcnt < GridWdt; hcnt++) {
        node = new Node();
        Nodes.add(node);
        node.Xloc = this.XOrg + hcnt * NodeSpacing;
        node.Yloc = this.YOrg + vcnt * NodeSpacing;
      }
    }
    RandomChangeRate = 1.0;
    RandomizeAllConnections();
    RandomChangeRate = 0.125;
  }
  /* ********************************************************************************************************* */
  public void Connect2Nodes(Node node0, Node node1) {
    double Distance = 1.0;// snox, simple for now, temporary.
    node0.AttachNeighbor(node1, Distance);
    node1.AttachNeighbor(node0, Distance);
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
  public void RandomizeAllConnections() {
    int X0, Y0, X1, Y1;
    int RegionWdt = this.GridWdt;
    int RegionHgt = this.GridHgt;
    int XMax = RegionWdt - 1;
    int YMax = RegionHgt - 1;
    for (int vcnt = 0; vcnt < RegionHgt; vcnt++) {
      Y0 = Math.max(vcnt - MaxConnectionRadius, 0);// clip Y
      Y1 = Math.min(vcnt + MaxConnectionRadius, YMax);
      for (int hcnt = 0; hcnt < RegionWdt; hcnt++) {
        X0 = Math.max(hcnt - MaxConnectionRadius, 0);// clip X
        X1 = Math.min(hcnt + MaxConnectionRadius, XMax);
        RandomizeNodeConnections(X0, Y0, hcnt, vcnt, X1, Y1);
      }
    }
  }
  /* ********************************************************************************************************* */
  public void RandomizeNodeConnections(int X0, int Y0, int XLoc, int YLoc, int X1, int Y1) {// Sparsely connect a node with its neighbors.
    double Azar;
    Node ctr = GetNodeFromXY(XLoc, YLoc);// get this from xloc and yloc
    ctr.CleanEverything();// clean out all dead neighbor links and routes that refer to them
    for (int vcnt = Y0; vcnt <= Y1; vcnt++) {
      for (int hcnt = X0; hcnt <= X1; hcnt++) {
        if (!((XLoc == hcnt) && (YLoc == vcnt))) {// do not connect to self
          Azar = rand.nextDouble();
          if (Azar <= RandomChangeRate) {
            Node nbr = GetNodeFromXY(hcnt, vcnt);
            Azar = rand.nextDouble();
            if (ctr.Neighbors.containsKey(nbr)) {
              if (Azar > ConnectionDensity) {
                Disconnect2Nodes(ctr, nbr);
              }
            } else {
              if (Azar <= ConnectionDensity) {
                Connect2Nodes(ctr, nbr);
              }
            }
          }
        }
      }
    }
  }
  /* ********************************************************************************************************* */
  public void CleanEverything() {// called once in a while
    for (int cnt = 0; cnt < this.Nodes.size(); cnt++) {
      Node node = this.Nodes.get(cnt);
      node.CleanEverything();
    }
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
  Node FindClosestNode(int MouseX, int MouseY) {
    double dx, dy, dist, MinDist = Double.MAX_VALUE;
    Node closest = null;
    int siz = Nodes.size();
    for (int cnt = 0; cnt < siz; cnt++) {// clunky inefficient way to find closest node
      Node nd = Nodes.get(cnt);
      dx = (nd.Xloc - MouseX);
      dy = (nd.Yloc - MouseY);
      dist = Math.sqrt(dx * dx + dy * dy);
      if (MinDist > dist) {
        MinDist = dist;
        closest = nd;
      }
    }
    return closest;
  }
  /* ********************************************************************************************************* */
  Node TriggerBlastPacket(int MouseX, int MouseY) {
    double dx, dy, dist, MinDist = Double.MAX_VALUE;
    Node closest = FindClosestNode(MouseX, MouseY);
    closest.LaunchMyOwnBlastPacket();
    return closest;
  }
  void BroadcastAllPackets() {
    int siz = Nodes.size();
    for (int cnt = 0; cnt < siz; cnt++) {
      Node nd = Nodes.get(cnt);
      nd.BroadcastAllBlastPackets();
    }
  }
  void ProcessInPacketBuffer() {
    int siz = Nodes.size();
    for (int cnt = 0; cnt < siz; cnt++) {
      Node nd = Nodes.get(cnt);
      nd.ProcessInPacketBuffer();
    }
  }
  void Seek(int MouseX, int MouseY, Node TargetNode) {
    Node seeker = FindClosestNode(MouseX, MouseY);
    seeker.SendPacketTo(TargetNode);
  }
}
