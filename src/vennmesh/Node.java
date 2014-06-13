package vennmesh;

/**

 @author MultiTool
 */
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* ********************************************************************************************************* */
public class Node extends Zone implements IDeleteable {
  public double Xloc, Yloc;
  public WorldList Earth;
  public Map<Node, NbrInfo> Neighbors;
  public Map<Zone, RouteEntry> Routing;
  public List<Zone> ZoneVector;// my address
    /* ********************************************************************************************************* */
  public Node() {
    Xloc = 0;
    Yloc = 0;
    Neighbors = new HashMap<Node, NbrInfo>();
    Routing = new HashMap<Zone, RouteEntry>();
    ZoneVector = new ArrayList<Zone>();// my address
  }
  /* ********************************************************************************************************* */
  public Node(WorldList Earth0) //: base()
  {
    this();
    this.Earth = Earth0;
  }
  /* ********************************************************************************************************* */
  public void SendBlastPacket() {// will be based on location and randomness
    Packet pkt = new Packet();
    Collection<NbrInfo> values = this.Neighbors.values();
    for (NbrInfo ninfo : values) {
      // snox here we should doctor the packet clone to increase its distance and intensity
      ninfo.Nbr.ReceiveBlastPacket(this, pkt.CloneMe());
    }
  }
  /* ********************************************************************************************************* */
  public void CheckNeighbor(Node nbr) {// find out if neighbor is connectable
    if (Earth.contains(nbr)) {
      if (this.CanConnectTo(nbr)) {
        return;
      }
    }
    this.Neighbors.get(nbr).MarkForRemoval();
  }
  /* ********************************************************************************************************* */
  public boolean CanConnectTo(Node other) {// will be based on location and randomness
    return true;
  }
  /* ********************************************************************************************************* */
  public void AttachNeighbor(Node nbr) {// This is called by the World
    NbrInfo nbrinfo;
    if (this.Neighbors.containsKey(nbr)) {
      nbrinfo = this.Neighbors.get(nbr);
      nbrinfo.Dead = false;
    } else {
      nbrinfo = new NbrInfo();
      this.Neighbors.put(nbr, nbrinfo);
    }
    nbrinfo.Nbr = nbr;
  }
  /* ********************************************************************************************************* */
  public void DisconnectNeighbor(Node nbr) {// This is called by the World
    NbrInfo nbrinfo;
    nbrinfo = this.Neighbors.get(nbr);
    nbrinfo.MarkForRemoval();
  }
  /* ********************************************************************************************************* */
  public void CleanEverything() {// called once in a while
    for (Map.Entry<Zone, RouteEntry> kvp0 : this.Routing.entrySet()) {
      RouteEntry endpoint = kvp0.getValue();
      if (endpoint.ClosestNbr.Dead) {
        endpoint.ClosestNbr.UnRefMe();
        /*
         if (endpoint.ClosestNbr.RefCount <= 0) {// if nbr is dead, AND no routes reference it, delete it.
         Neighbors.remove(endpoint.ClosestNbr.Nbr);
         }
         */
        endpoint.ClosestNbr = null;// guarantee a crash if bad code tries to use this again.
        this.Routing.remove(kvp0.getKey());
        endpoint.DeleteMe();
      }
    }

    Node[] keys = new Node[this.Neighbors.size()];
    this.Neighbors.keySet().toArray(keys);
    for (int cnt = 0; cnt < keys.length; cnt++) {
      Node key = keys[cnt];
      NbrInfo nbr = this.Neighbors.get(key);
      if (nbr.Dead) {
        if (nbr.RefCount <= 0) {// if nbr is dead, AND no routes reference it, delete it.
          Neighbors.remove(key);
          nbr.DeleteMe();
        }
      }
    }
    /*
     in C++, make this a flexray. then every node has an int ID and a place to go.
     packing means traversing the whole tree. 
     adding by ID is easy, removing by ID is easy. 

     so I get a real packet, check its origin ID.
     look it up in routing flexray
     if not found then reject packet
     if found then get the routing entry, then look up the neighbor in the neighbor table. (refcounts?)
     if nbrstat is deprecated then remove me from routing table(do this) // OR turn on this route entry's deprecate flag. 

     hm, all we saved in routing table was best nbr for forwarding. 
     if nbr is gone we done got nothing. fine, then either reject the packet (for now) or ask all remaining nbrs for a replacement. 

     next,
     add a nbr happens independently. not all nbrs are in routing table right away.
     handled high-level. a nbr and I are made aware of each other and we add entre si to our nbr tables. 

     VennoMesh
     VenoMesh
     VennRoad
     ViaVenn
     VennWay
     VennHorizon
     PlanetVenn
     VennScape
     VennSpace
     VennRoute

     Vennscape - best so far
     Venniverse
    
     */
  }
  /* ********************************************************************************************************* */
  private void SortAndCullRoutes(int NumToKeep) {// called once in a while
    ArrayList<RouteEntry> vals = new ArrayList(this.Routing.values());
    Collections.sort(vals, new EndpointFocusComparer());
    int NumToKill = vals.size() - NumToKeep;
    for (int cnt = 0; cnt < NumToKill; cnt++) {
      RouteEntry route = vals.get(cnt);
      RouteEntry RemovedRoute = this.Routing.remove(route);
      route.ClosestNbr.UnRefMe();
    }
  }
  /* ********************************************************************************************************* */
  @Override
  public void DeleteMe() {
    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  /* ********************************************************************************************************* */
  public class EndpointFocusComparer implements Comparator<RouteEntry> {
    @Override
    public int compare(RouteEntry c0, RouteEntry c1) {
      return (c0.FieldStrength > c1.FieldStrength ? -1 : (c0.FieldStrength == c1.FieldStrength ? 0 : 1));// ascending
      //return (c0.FieldStrength < c1.FieldStrength ? -1 : (c0.FieldStrength == c1.FieldStrength ? 0 : 1));// descending
    }
  }
  /* ********************************************************************************************************* */
  public void ReceiveRealPacket(Node nbr, Packet pkt) {// for routing and passing on real communication packets
    Packet PktNext = null;
    RouteEntry entry = null;
    NbrInfo NextHop = null;
    entry = this.Routing.get(pkt.Origin);
    NextHop = entry.ClosestNbr;
    PktNext = pkt.CloneMe();
    PktNext.Distance += NextHop.Distance;// snox packet receiver should probably inc distance on receipt. then actual measured time might be used.
    // next clone the packet, increment its distance by entry.ClosestNbr.Distance, and forward it to the NextHop nbr
  }
  /*
  
   ***********  we need a thing like ReceiveBlastPacket that processes packets to calculate FieldStrength 
  
   we can pass field strength with each blast, but it will only be relevant for the previous blast.
  
   difference is though that we need to use ALL packets from a blast to fully calc strength
  
   strength = clip( 2*(sum of FieldStrength of all packets of this packet ID)/(my total neighbors) )
  
   no no what we need to do is:
   poll all neighbors regarding a particular endpoint. 
   for everyone who is closer to that endpoint:
   pass their field strength through me (add them and mult by my focus, etc). make that my field strength.
  
   OR
  
   every time I get a packet from X nbr, it carries that nbr's field strength for that endpoint. (could be ALL endpoints at once, yes?)
   I add this field strength to a total. after Y time, I combine the total with my focus (etc) and publish my field strength.
   until then my strength with this endpoint is 0, or antiquated.
  
   does the field strength have to be from a single blast ID?
  
  
   */
  /* ********************************************************************************************************* */
  public Packet ReceiveBlastPacket(Node nbr, Packet pkt) {// process a packet for distance updates
    NbrInfo NbrRec = this.Neighbors.get(nbr);// nbr is the neighbor who handed me this packet
    RouteEntry entry;
    Packet PktNext = null;
    // when I get a blast packet, I look up its origin (as destination) in the routing table. 
    if (!this.Routing.containsKey(pkt.Origin)) {// if no entry is found, add it and forward the packet.
      // here is where we would flag if our routing table is too full
      entry = new RouteEntry();
      entry.ConsumePacket(NbrRec, pkt);
      entry.AddFieldStrength(pkt, this.Neighbors.size());
      this.Routing.put(pkt.Origin, entry);
      NbrRec.RefMe();
      PktNext = pkt.CloneMe();
      PktNext.Distance += NbrRec.Distance;// snox should probably inc distance *before* we call this function
    } else {
      entry = this.Routing.get(pkt.Origin);
      if (pkt.BirthTimeStamp > entry.BirthTimeStamp) {// if its birth time is more recent than the existing entry, replace the old entry and forward the packet.
        entry.ClosestNbr.UnRefMe();
        entry.ConsumePacket(NbrRec, pkt);
        entry.RolloverFieldStrength();// old packet is surpassed, close the deal
        entry.FieldSum = entry.NextFieldStrength = 0.0;
        entry.AddFieldStrength(pkt, this.Neighbors.size());

        NbrRec.RefMe();
        PktNext = pkt.CloneMe();
        PktNext.Distance += NbrRec.Distance;// snox should probably inc distance *before* we call this function
      } else if (pkt.BirthTimeStamp == entry.BirthTimeStamp) {// if its birth time is the same as the current entry
        if (pkt.Distance < entry.Distance) {// if it has better miles than the current entry, replace the current entry and forward the packet.
          entry.ClosestNbr.UnRefMe();
          entry.ConsumePacket(NbrRec, pkt);
          entry.AddFieldStrength(pkt, this.Neighbors.size());
          NbrRec.RefMe();
          PktNext = pkt.CloneMe();
          PktNext.Distance += NbrRec.Distance;// snox should probably inc distance *before* we call this function
        }
      }
    }
    // otherwise return null and the packet will be discarded.
    return PktNext;
  }
  /* ********************************************************************************************************* */
  public void CalcFieldFocus(RouteEntry EndPoint) {// Determine how much the gradient from a zone should fall off as it passes through me.
    double Dist;
    double NumCloser = 0;
    for (Map.Entry<Node, NbrInfo> kvp : this.Neighbors.entrySet()) {
      // add up all my neighbors who are closer to EndPoint than me
      NbrInfo ni = kvp.getValue();
      Dist = ni.Nbr.GetDistanceFrom(EndPoint.ClosestNbr.Nbr);
      if (Dist <= EndPoint.Distance) {
        NumCloser += 1.0;// this should really add the propagated force of each input nbr. weighted sum. 
      }
    }
    double Tightness = NumCloser / (double) this.Neighbors.size();
    EndPoint.FieldFocus = Math.min(Tightness * 2.0, 1.0);

    /*
     so it could be number of pipes inward, divided among the number of pipes outward. problem: if no pipes outward then force is infinity
     if no pipes inward then force is zero.
     as-is, number of pipes inward divided among all pipes going everywhere. 

     now what do we do with this information?

     I could keep the Focus/force to myself, and just use it to score my route endpoints for culling. 
     or, I could pass the value downstream to all neighbors further away from the source, 
     so they will multiply that diminished force by their own Focuss. yeah gotta do this latter.

     if evaluation is only local, then far away from a tiny zone will have equal power to a large zone. 

     so maybe every blast packet should also carry a Focus value? though the focus will only be yesterday's news, not from this particular blast.
     or, outsider nodes can initiate their own blast packets as soon as their distances are resolved? 

     (zone blasts network) -> (nodes resolve distances) -> (nodes calculate focus) -> (zone border blasts downstreamers with initial intensity of 1.0) -> 
     (downstreamers wait for all intensity signals to come in (time cutoff), multiply sum by own focus and divide among outputs, passing to further downstreamers)

     now and then, nodes sort and cull their routing tables. weak focus does not affect signal, it's just a relative term for sorting. 
      
     SumOfOutputPipes = TotalPipes-NumInputs;
     Nah, just do min( (SumOfInputPower)/(SumOfOutputPipes), 1.0) for each output pipe. 
     Hmm, min( 2*(SumOfInputPower)/(TotalPipes), 1.0) rises faster up to (half of all nbrs are inputs)

     */
  }
  /* ********************************************************************************************************* */
  private double GetDistanceFrom(Node EndPointNode) {
    RouteEntry EndPointInfo = this.Routing.get(EndPointNode);
    return EndPointInfo.Distance;
  }
  /* ********************************************************************************************************* */
  public void Draw_Me(Graphics2D g2d) {// Drawable
    double Radius = 3;
    double Diameter = Radius * 2;
    g2d.setColor(Color.red);
    g2d.fillOval((int) (this.Xloc - Radius), (int) (this.Yloc - Radius), (int) Diameter, (int) Diameter);
    NbrInfo ninf;
    Node nbr;// also draw lines to all of my neighbors.
    g2d.setColor(Color.cyan);
    for (Map.Entry<Node, NbrInfo> kvp : this.Neighbors.entrySet()) {
      ninf = kvp.getValue();
      if (!ninf.Dead) {
        nbr = ninf.Nbr;
        try {
          g2d.drawLine((int) this.Xloc, (int) this.Yloc, (int) nbr.Xloc, (int) nbr.Yloc);
        } catch (Exception ex) {
          boolean nop = true;
        }
      }
    }
  }
  /* ********************************************************************************************************* */
  public static class RouteEntry implements IDeleteable {
    public Node EndPointNode;// this is the same as the hash key that finds this record
    public int BirthTimeStamp;
    public double Distance;
    public NbrInfo ClosestNbr;
    public double FieldFocus;// The degree to which I promote the intensity of this endpoint's field. opposite of Beam Divergence (Diffraction?).  could also be called BeamCollimation? Collection? Convergence? Focus? 
    public double FieldStrength = 0.0;// calculated from FieldFocus * field that reaches me, for strength of this particular endpoint. FieldIntensity?
    public double NextFieldStrength = 0.0;// calculated from FieldFocus * field that reaches me, for strength of this particular endpoint. FieldIntensity?
    public double FieldSum = 0.0;// temporary
    public void ConsumePacket(Packet pkt) {
      this.BirthTimeStamp = pkt.BirthTimeStamp;// copy over best packet info
      this.Distance = pkt.Distance;
      this.EndPointNode = pkt.Origin;
    }
    public void ConsumePacket(NbrInfo nbr, Packet pkt) {
      this.ClosestNbr = nbr;// copy over best packet info
      this.ConsumePacket(pkt);
    }
    public void AddFieldStrength(Packet pkt, double NumNbrs) {
      this.FieldSum += pkt.FieldStrength;
      if (false) {
        this.FieldStrength += 2.0 * (pkt.FieldStrength / NumNbrs);
        this.FieldStrength = Math.min(1.0, this.FieldStrength);
      }
      this.NextFieldStrength += 2.0 * (pkt.FieldStrength / NumNbrs);
      this.NextFieldStrength = Math.min(1.0, this.NextFieldStrength);
    }
    public void RolloverFieldStrength() {
      this.FieldStrength = this.NextFieldStrength;
      this.NextFieldStrength = 0.0;
      this.FieldSum = 0.0;
    }
    @Override
    public void DeleteMe() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  /* ********************************************************************************************************* */
  public static class NbrInfo implements IDeleteable {
    public Node Nbr;
    public double Distance = Double.POSITIVE_INFINITY;
    public boolean Dead = false;
    public int RefCount = 0;
    public void MarkForRemoval() {
      Dead = true;
      Nbr = null;
    }
    public int RefMe() {
      return ++RefCount;
    }
    public int UnRefMe() {
      return --RefCount;
    }
    @Override
    public void DeleteMe() {
      //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
  /* ********************************************************************************************************* */
  public static class Packet implements IDeleteable {// this is a blast packet
    public int BirthTimeStamp;
    public double Distance;
    public double FieldStrength;
    public Node Origin;
    public Packet CloneMe() {
      Packet child = new Packet();
      child.BirthTimeStamp = this.BirthTimeStamp;
      child.Origin = this.Origin;
      child.Distance = this.Distance;
      child.FieldStrength = this.FieldStrength;
      return child;
    }
    @Override
    public void DeleteMe() {
      //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }
}
