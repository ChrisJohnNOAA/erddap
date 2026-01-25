package gov.noaa.pfel.erddap.jte;

public class YouAreHere {
  public int language;
  public String endOfRequest;
  public String userDapQuery;

  public YouAreHere(int language, String endOfRequest, String userDapQuery) {
    this.language = language;
    this.endOfRequest = endOfRequest;
    this.userDapQuery = userDapQuery;
  }
}
