package vennmesh;

/**

 @author MultiTool
 */
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/* ********************************************************************************************************* */
public class Node implements IDeleteable {
  public double Xloc, Yloc;
  public WorldList Earth;
  public Map<Node, NbrInfo> Neighbors;
  public Map<Zone, RouteEntry> Routing;
  public Zone MeZone;
  private List<Zone> MyAddressVector;// my address
  public LinkedList<DataPacket> DataPacketInBuf, DataPacketOutBuf;
  //public LinkedList<Packet> PacketInBuf, PacketOutBuf;
  public LinkedList<BlastPacket> BlastPacketInBuf, BlastPacketOutBuf;
  public Color MyColor;
  /* ********************************************************************************************************* */
  public Node() {
    Xloc = 0;
    Yloc = 0;
    this.MeZone = new Zone();
    MyAddressVector = new ArrayList<Zone>();// my address
    MyAddressVector.add(MeZone);// I am always a member of my own personal zone

    Neighbors = new HashMap<Node, NbrInfo>();
    Routing = new HashMap<Zone, RouteEntry>();
    BlastPacketInBuf = new LinkedList<BlastPacket>();
    BlastPacketOutBuf = new LinkedList<BlastPacket>();
    DataPacketInBuf = new LinkedList<DataPacket>();
    DataPacketOutBuf = new LinkedList<DataPacket>();
  }
  /* ********************************************************************************************************* */
  public Node(WorldList Earth0) //: base()
  {
    this();
    this.Earth = Earth0;
  }
  /* ********************************************************************************************************* */
  public void JoinZone(Zone zoneid) {// join a zip code/zone/subnet
    if (!MyAddressVector.contains(zoneid)) {
      MyAddressVector.add(zoneid);
      if (this.Routing.containsKey(zoneid)) {// I cannot have gradients to a zone I am part of
        this.Routing.remove(zoneid);
      }
    }
  }
  /* ********************************************************************************************************* */
  public void ReceiveBlastPacketToBuffer(BlastPacket pkt) {// cache a packet for distance updates
    BlastPacketInBuf.add(pkt);
  }
  /* ********************************************************************************************************* */
  public void ReceiveDataPacketToBuffer(DataPacket pkt) {// cache a data packet
    DataPacketInBuf.add(pkt);
  }
  /* ********************************************************************************************************* */
  public void LaunchMyOwnBlastPacket() {// emit my own packet to give everyone else my metrics
    LaunchMyOwnBlastPacket(this.MeZone, 10000);
  }
  /* ********************************************************************************************************* */
  public void LaunchMyOwnBlastPacket(Zone StartZone, int PacketLifeSpan) {// emit my own packet to give everyone else the metrics of one of my address zones
    BlastPacket pkt = new BlastPacket();
    pkt.BirthTimeStamp = VennMesh.GetTimeStamp();
    pkt.SerialNumber = StartZone.LatestSerialNumber;//.GetLatestSerialNumber();
    pkt.OriginZone = StartZone;
    pkt.Distance = 0;
    pkt.FieldStrength = 1.0;// if we are initiating our own blast, field strength is 1. 
    pkt.LatestSender = this;
    pkt.LifeSpan = PacketLifeSpan;
    BlastPacketOutBuf.add(pkt);
  }
  /* ********************************************************************************************************* */
  public void ProcessInPacketBuffer() {// process a packet for distance updates
    BlastPacket bpkt, BlastPktNext;
    DataPacket dpkt, DataPktNext;
    while (!BlastPacketInBuf.isEmpty()) {
      bpkt = BlastPacketInBuf.removeFirst(); // if (TempPkt instanceof BlastPacket) { } ?
      BlastPktNext = ProcessBlastPacket(bpkt);
      bpkt.DeleteMe();
      if (BlastPktNext != null) {
        if (BlastPktNext.LifeSpan > 0) {// snox, fiat lifespan is just for testing
          BlastPacketOutBuf.add(BlastPktNext);
        }
      }
    }
    while (!DataPacketInBuf.isEmpty()) {
      dpkt = DataPacketInBuf.removeFirst(); // if (TempPkt instanceof BlastPacket) { } ?
      DataPktNext = ProcessDataPacket(dpkt);
      dpkt.DeleteMe();
      if (DataPktNext != null) {
        DataPacketOutBuf.add(DataPktNext);
      }
    }
  }
  /* ********************************************************************************************************* */
  private DataPacket ProcessDataPacket(DataPacket dpkt) {
    DataPacket retval = null;
    if (dpkt.UltimateDestination == this.MeZone) {
      // data found me! stop doing everything and eat the data!
      this.MyColor = new Color(0.0f, 1.0f, 0.0f);// bright green
      return retval;
    }
    int FirstZoneDex = dpkt.ZoneDex;
    while (!this.Routing.containsKey(dpkt.CurrentDestination)) {
      dpkt.RotateTarget();// keep shifting packet targets until we find one that we know about
      if (dpkt.ZoneDex == FirstZoneDex) {
        break;// snox, not thought out
      }
    }
    //if (dpkt.ZoneDex != FirstZoneDex) {
    if (this.Routing.containsKey(dpkt.CurrentDestination)) {
      retval = dpkt.CloneMe();
    }
    return retval;
  }
  /* ********************************************************************************************************* */
  public void SendAllDataPackets() {
    RouteEntry MyKnowledgeOfDestination;
    DataPacket pkt;// go through all waiting outpackets, and send each to appropriate neighbor.
    while (!DataPacketOutBuf.isEmpty()) {
      pkt = DataPacketOutBuf.removeFirst();
      if (this.Routing.containsKey(pkt.CurrentDestination)) {
        MyKnowledgeOfDestination = this.Routing.get(pkt.CurrentDestination);
        NbrInfo ToNbr = MyKnowledgeOfDestination.ClosestNbr;
        ToNbr.ReceiveDataPacketToBuffer(pkt);
      }
    }
  }
  /* ********************************************************************************************************* */
  public void BroadcastAllBlastPackets() {
    BlastPacket pkt;// go through all waiting outpackets, and send each to all neighbors.
    while (!BlastPacketOutBuf.isEmpty()) {
      pkt = BlastPacketOutBuf.removeFirst();
      BroadcastBlastPacket(pkt);
    }
  }
  /* ********************************************************************************************************* */
  public BlastPacket ProcessBlastPacket(BlastPacket pkt) {// process a packet for distance updates
    BlastPacket PktNext = null;
    Node nbr = pkt.LatestSender;
    //pkt.LatestSender = this;//?
    NbrInfo FromNbr = this.Neighbors.get(nbr);// nbr is the neighbor who handed me this packet

    if (this.MyAddressVector.contains(pkt.OriginZone)) {
      /*
       first, if I am a member of the same zone that launched this packet, disregard it. no gradients of a zone inside that zone.
       what happens if a node disobeys this?
       */
      return null;
    }

    //pkt.Distance += FromNbr.Distance;// now packet's distance will be my distance from endpoint
    RouteEntry MyKnowledgeOfEndpoint;
    // when I get a blast packet, I look up its origin (as destination) in the routing table. 
    if (!this.Routing.containsKey(pkt.OriginZone)) {// if no entry is found, add it and forward the packet.
      // here is where we would flag if our routing table is too full
      MyKnowledgeOfEndpoint = new RouteEntry();
      MyKnowledgeOfEndpoint.ConsumePacket(FromNbr, pkt);
      MyKnowledgeOfEndpoint.AddFieldStrength(pkt, this.Neighbors.size());
      this.Routing.put(pkt.OriginZone, MyKnowledgeOfEndpoint);
      FromNbr.RefMe();
      PktNext = pkt.CloneMe();
      PktNext.UpdateForResend(this, FromNbr.Distance, MyKnowledgeOfEndpoint.FieldStrength);// now child's field strength will be the strength of field at me .FieldFocus
      //PktNext.LatestSender = this; PktNext.FieldStrength *= MyKnowledgeOfEndpoint.FieldFocus;// now child's field strength will be the strength of field at me
    } else {
      MyKnowledgeOfEndpoint = this.Routing.get(pkt.OriginZone);
      if (pkt.SerialNumber > MyKnowledgeOfEndpoint.LatestSerialNumber) {// if its birth time is more recent than the existing entry, replace the old entry and forward the packet.
        MyKnowledgeOfEndpoint.ClosestNbr.UnRefMe();
        MyKnowledgeOfEndpoint.ConsumePacket(FromNbr, pkt);
        MyKnowledgeOfEndpoint.RolloverFieldStrength();// old packet is surpassed, close the deal?
        MyKnowledgeOfEndpoint.AddFieldStrength(pkt, this.Neighbors.size());
        FromNbr.RefMe();
        PktNext = pkt.CloneMe();
        PktNext.UpdateForResend(this, FromNbr.Distance, MyKnowledgeOfEndpoint.FieldStrength);// now child's field strength will be the strength of field at me .FieldFocus
        //PktNext.LatestSender = this; PktNext.FieldStrength *= MyKnowledgeOfEndpoint.FieldFocus;// now child's field strength will be the strength of field at me
      } else if (pkt.SerialNumber == MyKnowledgeOfEndpoint.LatestSerialNumber) {// if its birth time is the same as the current entry
        if (pkt.Distance < MyKnowledgeOfEndpoint.Distance) {// if it has better miles than the current entry, replace the current entry and forward the packet.
          MyKnowledgeOfEndpoint.ClosestNbr.UnRefMe();
          MyKnowledgeOfEndpoint.ConsumePacket(FromNbr, pkt);
          MyKnowledgeOfEndpoint.AddFieldStrength(pkt, this.Neighbors.size());
          FromNbr.RefMe();
          PktNext = pkt.CloneMe();
          PktNext.UpdateForResend(this, FromNbr.Distance, MyKnowledgeOfEndpoint.FieldStrength);// now child's field strength will be the strength of field at me .FieldFocus
          //PktNext.LatestSender = this; PktNext.FieldStrength *= MyKnowledgeOfEndpoint.FieldFocus;// now child's field strength will be the strength of field at me
        }
      }
    }
    // otherwise return null and the packet will be discarded.
    return PktNext;
  }
  /* ********************************************************************************************************* */
  public void BroadcastBlastPacket(BlastPacket pkt) {
    Collection<NbrInfo> values = this.Neighbors.values();
    BlastPacket child;
    for (NbrInfo ninfo : values) {// pass to all neighbors
      child = pkt.CloneMe();
      ninfo.ReceiveBlastPacketToBuffer(child);
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
  public void AttachNeighbor(Node nbr, double Distance) {// This is called by the World
    NbrInfo nbrinfo;
    if (this.Neighbors.containsKey(nbr)) {
      nbrinfo = this.Neighbors.get(nbr);
      nbrinfo.Dead = false;
    } else {
      nbrinfo = new NbrInfo();
      nbrinfo.MyNode = this;
      this.Neighbors.put(nbr, nbrinfo);
    }
    nbrinfo.Distance = Distance;
    nbrinfo.Nbr = nbr;
  }
  /* ********************************************************************************************************* */
  public void DisconnectNeighbor(Node nbr) {// This is called by the World
    NbrInfo nbrinfo;
    nbrinfo = this.Neighbors.get(nbr);
    try {
      nbrinfo.MarkForRemoval();
    } catch (Exception ex) {
      boolean nop = true;
    }
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
    /* call DeleteMe on all of these:
     Map<Node, NbrInfo> Neighbors;
     Map<Zone, RouteEntry> Routing;
     Zone MeZone;
     List<Zone> MyAddressVector;// my address
     LinkedList<DataPacket> DataPacketInBuf, DataPacketOutBuf;
     LinkedList<BlastPacket> BlastPacketInBuf, BlastPacketOutBuf;
     */
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
  void SendPacketToZone(Zone TargetZone) {
    DataPacket dpkt = new DataPacket();
    {// hack. long run targetzone will be targetnode, and node will give an address vector of zones to the data packet
      ArrayList<Zone> zvector = new ArrayList<Zone>();
      zvector.add(TargetZone);
      dpkt.AssignDestinationVector(zvector);
      dpkt.UltimateDestination = TargetZone;// this is wrong. ultimate dest can only be a single node, not a zone
    }
    this.DataPacketOutBuf.add(dpkt);
  }
  /* ********************************************************************************************************* */
  void SendPacketToNode(Node TargetNode) {
    DataPacket dpkt = new DataPacket();
    dpkt.AssignDestinationVector(TargetNode.MyAddressVector);
    dpkt.UltimateDestination = TargetNode.MeZone;// this is wrong. ultimate dest can only be a single node, not a zone
    //this.DataPacketOutBuf.add(dpkt);
    this.DataPacketInBuf.add(dpkt);
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
    double Radius = 12;
    double Diameter = Radius * 2;

    if (this.Routing.size() > 0) {
      ArrayList<RouteEntry> vals = new ArrayList(this.Routing.values());
      float fstrength = (float) (vals.get(0).FieldStrength);
      if (fstrength > 1.0) {
        boolean nop = true;
      }
      //System.out.println("" + fstrength + ";");
      //Color MyColor = Color.getHSBColor(fstrength, fstrength, fstrength);
      MyColor = new Color(fstrength, fstrength, fstrength);
      if (fstrength > 1.0 / 4.0) {
        MyColor = new Color(fstrength, 0.0f, 0.0f);
      } else if (fstrength > 1.0 / 16.0) {
        MyColor = new Color(0.0f, fstrength * 4.0f, 0.0f);
      } else if (fstrength > 1.0 / 64.0) {
        MyColor = new Color(0.0f, 0.0f, fstrength * 16.0f);
      } else if (fstrength > 0.0) {// 1.0 / (4.0 * 64.0)) {
        MyColor = new Color(fstrength * 64.0f, 0.0f, 0.0f);
      } else {
        MyColor = Color.yellow;
      }

      g2d.setColor(MyColor);
      if (false) {
        if (fstrength > 0) {
          g2d.setColor(Color.yellow);
        } else {
          g2d.setColor(Color.black);
        }
      }
    } else {
      g2d.setColor(Color.red);
    }
    g2d.fillOval((int) (this.Xloc - Radius), (int) (this.Yloc - Radius), (int) Diameter, (int) Diameter);
    NbrInfo ninf;
    g2d.setColor(Color.cyan);
    for (Map.Entry<Node, NbrInfo> kvp : this.Neighbors.entrySet()) {
      ninf = kvp.getValue();
      ninf.Draw_Me(g2d);// also draw lines to all of my neighbors.
    }
  }
  /* ********************************************************************************************************* */
  public static class RouteEntry implements IDeleteable {
    public Zone EndPointNode;// this is the same as the hash key that finds this record
    public int LatestSerialNumber;
    public int BirthTimeStamp;
    public double Distance;
    public NbrInfo ClosestNbr;
    public double FieldFocus;// The degree to which I promote the intensity of this endpoint's field. opposite of Beam Divergence (Diffraction?).  could also be called BeamCollimation? Collection? Convergence? Focus? 
    public double FieldStrength = 0.0;// calculated from FieldFocus * field that reaches me, for strength of this particular endpoint. FieldIntensity?
    public double NextFieldStrength = 0.0;// calculated from FieldFocus * field that reaches me, for strength of this particular endpoint. FieldIntensity?
    public double FieldSum = 0.0;// temporary
    public void ConsumePacket(BlastPacket pkt) {
      this.LatestSerialNumber = pkt.SerialNumber;// copy over best packet info
      this.Distance = pkt.Distance;
      this.EndPointNode = pkt.OriginZone;
      // and FieldStrength?
    }
    public void ConsumePacket(NbrInfo nbr, BlastPacket pkt) {
      this.ClosestNbr = nbr;// copy over best packet info
      this.ConsumePacket(pkt);
    }
    public void AddFieldStrength(BlastPacket pkt, double NumNbrs) {
      this.FieldSum += pkt.FieldStrength;
      this.NextFieldStrength += 2.0 * (pkt.FieldStrength / NumNbrs);
      //this.NextFieldStrength += 1.0 * (pkt.FieldStrength / NumNbrs);
      this.NextFieldStrength = Math.min(1.0, this.NextFieldStrength);
    }
    public void RolloverFieldStrength() {
      this.FieldStrength = this.NextFieldStrength;
      this.NextFieldStrength = 0.0;
      this.FieldSum = 0.0;
    }
    @Override
    public void DeleteMe() {
      EndPointNode = null;// mess up all these values so that if this object is reused it will throw an error
      LatestSerialNumber = Integer.MIN_VALUE;
      Distance = Double.POSITIVE_INFINITY;
      ClosestNbr = null;
      FieldFocus = Double.NEGATIVE_INFINITY;
      FieldStrength = Double.NEGATIVE_INFINITY;
      NextFieldStrength = Double.NEGATIVE_INFINITY;
      FieldSum = Double.NEGATIVE_INFINITY;
    }
  }
  /* ********************************************************************************************************* */
  public static class NbrInfo implements IDeleteable {
    public Node Nbr;
    public Node MyNode;
    public double Distance = Double.POSITIVE_INFINITY;
    public boolean Dead = false;
    public int RefCount = 0;
    public Color MyColor;
    public Stroke MyStroke;
    public NbrInfo() {
      MyColor = Color.cyan;
      MyStroke = new BasicStroke(1);
    }
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
    public void ReceiveBlastPacketToBuffer(BlastPacket pkt) {
      if (!this.Dead) {
        //MyColor = Color.getHSBColor(Display.rand.nextFloat(), Display.rand.nextFloat(), Display.rand.nextFloat());
        MyColor = new Color(Display.rand.nextFloat(), Display.rand.nextFloat(), Display.rand.nextFloat());
        MyStroke = new BasicStroke(1);
        this.Nbr.ReceiveBlastPacketToBuffer(pkt);
      }
    }
    public void ReceiveDataPacketToBuffer(DataPacket pkt) {
      if (!this.Dead) {
        MyColor = Color.red;
        MyStroke = new BasicStroke(4);
        //MyColor = Color.getHSBColor(Display.rand.nextFloat(), Display.rand.nextFloat(), Display.rand.nextFloat());
        this.Nbr.ReceiveDataPacketToBuffer(pkt);
      }
    }
    @Override
    public void DeleteMe() {
      Nbr = null;
      MyNode = null;
      Distance = Double.POSITIVE_INFINITY;
    }
    /* ********************************************************************************************************* */
    public void Draw_Me(Graphics2D g2d) {// Drawable
      if (!this.Dead) {// draw lines to all of my neighbors.
        g2d.setColor(MyColor);
        g2d.setStroke(MyStroke);
        g2d.drawLine((int) MyNode.Xloc, (int) MyNode.Yloc, (int) Nbr.Xloc, (int) Nbr.Yloc);
      }
    }
  }
  /* ********************************************************************************************************* */
  public enum PacketTypeEnum {
    None, DataPacket, BlastPacket
  };
  /* ********************************************************************************************************* */
  public static class Packet implements IDeleteable {// this is a blast packet
    public PacketTypeEnum MyType;
    public int SerialNumber;
    public int BirthTimeStamp;
    public Zone OriginZone;
    public Node LatestSender;// most recent node who forwarded me
    public Packet() {
      this.SerialNumber = VennMesh.GetTimeStamp();
    }
    public void CopyVals(Packet donor) {
      this.MyType = donor.MyType;
      this.SerialNumber = donor.SerialNumber;
      this.OriginZone = donor.OriginZone;
      this.LatestSender = donor.LatestSender;
    }
    public Packet CloneMe() {
      BlastPacket child = new BlastPacket();
      child.CopyVals(this);
      return child;
    }
    @Override
    public void DeleteMe() {// mess up all these values so that if this object is reused it will throw an error
      SerialNumber = Integer.MIN_VALUE;
      OriginZone = null;
      LatestSender = null;
    }
  }
  /* ********************************************************************************************************* */
  public static class DataPacket extends Packet {// this is a blast packet
    public Zone CurrentDestination;
    public Zone UltimateDestination;// is this necessary?
    public List<Zone> DestAddressVector;// Destination address
    public int ZoneDex;
    public DataPacket() {//super();
      MyType = PacketTypeEnum.DataPacket;
      ZoneDex = 0;
      DestAddressVector = new ArrayList<Zone>();
    }
    public void AssignDestinationVector(List<Zone> AddressVector) {
      Zone zoneid;
      int AddrLen = AddressVector.size();
      for (int cnt = 0; cnt < AddrLen; cnt++) {
        zoneid = AddressVector.get(cnt).CloneMe();
        DestAddressVector.add(zoneid);
      }
      CurrentDestination = DestAddressVector.get(ZoneDex);
    }
    public void RotateTarget() {
      ZoneDex++;
      if (ZoneDex >= DestAddressVector.size()) {
        ZoneDex = 0;
      }
      CurrentDestination = DestAddressVector.get(ZoneDex);
    }
    public void CopyValsData(DataPacket donor) {
      this.CopyVals(donor);
      Zone parentzone, childzone;
      int NumZones = donor.DestAddressVector.size();
      for (int cnt = 0; cnt < NumZones; cnt++) {
        parentzone = donor.DestAddressVector.get(cnt);
        childzone = parentzone.CloneMe();
        this.DestAddressVector.add(childzone);
      }
      this.CurrentDestination = donor.CurrentDestination;
    }
    @Override
    public DataPacket CloneMe() {
      DataPacket child = new DataPacket();
      child.CopyValsData(this);
      return child;
    }
    @Override
    public void DeleteMe() {
      super.DeleteMe();
      CurrentDestination = null;
    }
  }
  /* ********************************************************************************************************* */
  public static class BlastPacket extends Packet {// this is a blast packet
    public double Distance;
    public double FieldStrength;
    public int LifeSpan;
    public BlastPacket() {
      //super();
      MyType = PacketTypeEnum.BlastPacket;
    }
    @Override
    public BlastPacket CloneMe() {
      BlastPacket child = new BlastPacket();
      child.CopyVals(this);
      child.Distance = this.Distance;
      child.FieldStrength = this.FieldStrength;
      child.LifeSpan = this.LifeSpan;
      return child;
    }
    public void UpdateForResend(Node Sender, double NextDist, double FieldFocus) {
      this.LatestSender = Sender;
      this.Distance += NextDist;// now packet's distance will be my distance from endpoint
      //this.FieldStrength *= FieldFocus;// now child's field strength will be the strength of field at me
      this.FieldStrength = FieldFocus;
      this.LifeSpan--;// just for experimenting, forced expiration of packet
    }
    @Override
    public void DeleteMe() {
      super.DeleteMe();
      Distance = Double.POSITIVE_INFINITY;
      FieldStrength = Double.NEGATIVE_INFINITY;
    }
  }
}

/*
  
 every time I get a packet from X nbr, it carries that nbr's field strength for that endpoint. (could be ALL endpoints at once, yes?)
 I add this field strength to a total. after Y time, I combine the total with my focus (etc) and publish my field strength.
 until then my strength with this endpoint is 0, or antiquated.
  
 */
