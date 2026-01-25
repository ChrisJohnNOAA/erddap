package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.dataset.EDDTable;

public class Subset {
  public int language;
  public String tErddapUrl;
  public EDDTable eddTable;
  public String loggedInAs;
  public String request;
  public String userDapQuery;
  public String endOfRequest;
  public YouAreHere youAreHere;

  public Subset(
      int language,
      String tErddapUrl,
      EDDTable eddTable,
      String loggedInAs,
      String request,
      String userDapQuery,
      String endOfRequest,
      YouAreHere youAreHere) {
    this.language = language;
    this.tErddapUrl = tErddapUrl;
    this.eddTable = eddTable;
    this.loggedInAs = loggedInAs;
    this.request = request;
    this.userDapQuery = userDapQuery;
    this.endOfRequest = endOfRequest;
    this.youAreHere = youAreHere;
  }
}
