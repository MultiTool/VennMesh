/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vennmesh;

/**

 @author MultiTool
 */
public class Zone implements IDeleteable {//ZoneId
  public String ZoneName;
  int ZoneId;
  public Zone() {
    this.ZoneId = VennMesh.GetNewId();
  }
  @Override
  public void DeleteMe() {
  }
  @Override
  public int hashCode() {
    return ZoneId;
  }
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Zone other = (Zone) obj;
    if (this.ZoneId != other.ZoneId) {
      return false;
    }
    return true;
  }
}
